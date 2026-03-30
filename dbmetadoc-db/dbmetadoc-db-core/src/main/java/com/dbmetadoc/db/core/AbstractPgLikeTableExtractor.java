package com.dbmetadoc.db.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PG-like 表结构提取基类。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Slf4j
public abstract class AbstractPgLikeTableExtractor extends AbstractJdbcMetadataExtractor {

    @Override
    protected String resolveCatalog(DatabaseConnectionInfo connectionInfo) {
        return connectionInfo.getDatabase();
    }

    @Override
    protected String resolveSchemaPattern(Connection connection, DatabaseConnectionInfo connectionInfo) {
        return StrUtil.blankToDefault(connectionInfo.getSchema(), "public");
    }

    @Override
    public DatabaseInfo extract(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        long startTime = System.currentTimeMillis();
        String schema = resolveSchemaPattern(connection, connectionInfo);
        log.info("开始使用 PG-like 目录提取元数据，数据库类型：{}，数据库：{}，Schema：{}",
                getDatabaseType().name(), connectionInfo.getDatabase(), schema);

        Map<String, TableInfo> tableMap = loadTables(connection, schema);
        loadColumns(connection, schema, tableMap);
        loadPrimaryKeys(connection, schema, tableMap);
        loadIndexes(connection, schema, tableMap);
        loadForeignKeys(connection, schema, tableMap);

        DatabaseInfo databaseInfo = DatabaseInfo.builder()
                .name(connectionInfo.getDatabase())
                .type(getDatabaseType().name())
                .version(connection.getMetaData().getDatabaseProductVersion())
                .driverName(connection.getMetaData().getDriverName())
                .databaseName(connectionInfo.getDatabase())
                .schemaName(schema)
                .catalogName(connectionInfo.getDatabase())
                .tables(new ArrayList<>(tableMap.values()))
                .build();
        enrichDatabaseInfo(connection, connectionInfo, databaseInfo);

        log.info("PG-like 元数据提取完成，数据库类型：{}，表数量：{}，耗时：{} ms",
                getDatabaseType().name(), tableMap.size(), System.currentTimeMillis() - startTime);
        return databaseInfo;
    }

    @Override
    protected void enrichDatabaseInfo(Connection connection, DatabaseConnectionInfo connectionInfo, DatabaseInfo databaseInfo)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet encoding = statement.executeQuery("SHOW server_encoding")) {
            if (encoding.next()) {
                databaseInfo.setCharset(encoding.getString(1));
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT datcollate FROM pg_database WHERE datname = current_database()");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                databaseInfo.setCollation(rs.getString("datcollate"));
            }
        }
    }

    protected Map<String, TableInfo> loadTables(Connection connection, String schema) throws SQLException {
        Map<String, TableInfo> tableMap = new LinkedHashMap<>();
        String sql = """
                SELECT n.nspname AS table_schema,
                       c.relname AS table_name,
                       COALESCE(d.description, '') AS table_comment,
                       c.relkind AS rel_kind
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0
                WHERE c.relkind IN ('r', 'p', 'f')
                  AND n.nspname = ?
                ORDER BY c.relname
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = TableInfo.builder()
                            .name(rs.getString("table_name"))
                            .comment(StrUtil.trimToNull(rs.getString("table_comment")))
                            .schema(rs.getString("table_schema"))
                            .tableType(mapTableType(rs.getString("rel_kind")))
                            .columns(new ArrayList<>())
                            .indexes(new ArrayList<>())
                            .foreignKeys(new ArrayList<>())
                            .build();
                    tableMap.put(buildTableKey(tableInfo.getSchema(), tableInfo.getName()), tableInfo);
                }
            }
        }
        return tableMap;
    }

    protected void loadColumns(Connection connection, String schema, Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT n.nspname AS table_schema,
                       c.relname AS table_name,
                       a.attname AS column_name,
                       format_type(a.atttypid, a.atttypmod) AS data_type,
                       col.character_maximum_length AS char_length,
                       col.numeric_precision AS numeric_precision,
                       col.numeric_scale AS numeric_scale,
                       NOT a.attnotnull AS is_nullable,
                       pg_get_expr(def.adbin, def.adrelid) AS default_value,
                       COALESCE(d.description, '') AS column_comment,
                       a.attnum AS ordinal_position,
                       a.attidentity AS identity_flag,
                       a.attgenerated AS generated_flag
                FROM pg_attribute a
                JOIN pg_class c ON c.oid = a.attrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                LEFT JOIN information_schema.columns col
                  ON col.table_schema = n.nspname
                 AND col.table_name = c.relname
                 AND col.column_name = a.attname
                LEFT JOIN pg_attrdef def ON def.adrelid = c.oid AND def.adnum = a.attnum
                LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = a.attnum
                WHERE c.relkind IN ('r', 'p', 'f')
                  AND a.attnum > 0
                  AND NOT a.attisdropped
                  AND n.nspname = ?
                ORDER BY c.relname, a.attnum
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = tableMap.get(buildTableKey(rs.getString("table_schema"), rs.getString("table_name")));
                    if (tableInfo == null) {
                        continue;
                    }
                    String defaultValue = rs.getString("default_value");
                    ColumnInfo columnInfo = ColumnInfo.builder()
                            .name(rs.getString("column_name"))
                            .type(rs.getString("data_type"))
                            .length(getInteger(rs, "char_length"))
                            .precision(getInteger(rs, "numeric_precision"))
                            .scale(getInteger(rs, "numeric_scale"))
                            .nullable(rs.getBoolean("is_nullable"))
                            .primaryKey(Boolean.FALSE)
                            .autoIncrement(isAutoIncrement(defaultValue, rs.getString("identity_flag")))
                            .generated(StrUtil.isNotBlank(rs.getString("generated_flag")))
                            .defaultValue(defaultValue)
                            .comment(StrUtil.trimToNull(rs.getString("column_comment")))
                            .ordinalPosition(getInteger(rs, "ordinal_position"))
                            .rawType(rs.getString("data_type"))
                            .build();
                    tableInfo.getColumns().add(completeColumn(columnInfo));
                }
            }
        }
    }

    protected void loadPrimaryKeys(Connection connection, String schema, Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT n.nspname AS table_schema,
                       c.relname AS table_name,
                       a.attname AS column_name
                FROM pg_constraint con
                JOIN pg_class c ON c.oid = con.conrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN LATERAL unnest(con.conkey) WITH ORDINALITY AS cols(attnum, ordinality) ON TRUE
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = cols.attnum
                WHERE con.contype = 'p'
                  AND n.nspname = ?
                ORDER BY c.relname, cols.ordinality
                """;
        Map<String, List<String>> primaryKeyMap = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableKey = buildTableKey(rs.getString("table_schema"), rs.getString("table_name"));
                    primaryKeyMap.computeIfAbsent(tableKey, key -> new ArrayList<>()).add(rs.getString("column_name"));
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : primaryKeyMap.entrySet()) {
            TableInfo tableInfo = tableMap.get(entry.getKey());
            if (tableInfo == null) {
                continue;
            }
            tableInfo.setPrimaryKey(String.join(",", entry.getValue()));
            if (CollUtil.isEmpty(tableInfo.getColumns())) {
                continue;
            }
            for (ColumnInfo columnInfo : tableInfo.getColumns()) {
                if (entry.getValue().contains(columnInfo.getName())) {
                    columnInfo.setPrimaryKey(Boolean.TRUE);
                }
            }
        }
    }

    protected void loadIndexes(Connection connection, String schema, Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT n.nspname AS table_schema,
                       c.relname AS table_name,
                       idx_cls.relname AS index_name,
                       idx.indisunique AS is_unique,
                       am.amname AS index_type,
                       string_agg(att.attname, ',' ORDER BY cols.ordinality) AS column_names
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN pg_index idx ON idx.indrelid = c.oid
                JOIN pg_class idx_cls ON idx_cls.oid = idx.indexrelid
                JOIN pg_am am ON am.oid = idx_cls.relam
                LEFT JOIN LATERAL unnest(idx.indkey) WITH ORDINALITY AS cols(attnum, ordinality) ON cols.attnum > 0
                LEFT JOIN pg_attribute att ON att.attrelid = c.oid AND att.attnum = cols.attnum
                WHERE c.relkind IN ('r', 'p', 'f')
                  AND NOT idx.indisprimary
                  AND n.nspname = ?
                GROUP BY n.nspname, c.relname, idx_cls.relname, idx.indisunique, am.amname
                ORDER BY c.relname, idx_cls.relname
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = tableMap.get(buildTableKey(rs.getString("table_schema"), rs.getString("table_name")));
                    if (tableInfo == null) {
                        continue;
                    }
                    tableInfo.getIndexes().add(IndexInfo.builder()
                            .name(rs.getString("index_name"))
                            .tableName(rs.getString("table_name"))
                            .unique(rs.getBoolean("is_unique"))
                            .columnNames(splitColumns(rs.getString("column_names")))
                            .type(StrUtil.upperFirst(StrUtil.blankToDefault(rs.getString("index_type"), "BTREE")))
                            .build());
                }
            }
        }
    }

    protected void loadForeignKeys(Connection connection, String schema, Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT con.conname AS fk_name,
                       n.nspname AS table_schema,
                       c.relname AS table_name,
                       src.attname AS column_name,
                       ref_cls.relname AS referenced_table,
                       ref.attname AS referenced_column
                FROM pg_constraint con
                JOIN pg_class c ON c.oid = con.conrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN pg_class ref_cls ON ref_cls.oid = con.confrelid
                JOIN LATERAL unnest(con.conkey, con.confkey) WITH ORDINALITY AS cols(src_attnum, ref_attnum, ordinality) ON TRUE
                JOIN pg_attribute src ON src.attrelid = c.oid AND src.attnum = cols.src_attnum
                JOIN pg_attribute ref ON ref.attrelid = ref_cls.oid AND ref.attnum = cols.ref_attnum
                WHERE con.contype = 'f'
                  AND n.nspname = ?
                ORDER BY c.relname, con.conname, cols.ordinality
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = tableMap.get(buildTableKey(rs.getString("table_schema"), rs.getString("table_name")));
                    if (tableInfo == null) {
                        continue;
                    }
                    tableInfo.getForeignKeys().add(ForeignKeyInfo.builder()
                            .name(rs.getString("fk_name"))
                            .tableName(rs.getString("table_name"))
                            .columnName(rs.getString("column_name"))
                            .referencedTable(rs.getString("referenced_table"))
                            .referencedColumn(rs.getString("referenced_column"))
                            .build());
                }
            }
        }
    }

    protected boolean isAutoIncrement(String defaultValue, String identityFlag) {
        return StrUtil.isNotBlank(identityFlag)
                || StrUtil.containsIgnoreCase(defaultValue, "nextval")
                || StrUtil.containsIgnoreCase(defaultValue, "identity");
    }

    protected String mapTableType(String relKind) {
        return switch (StrUtil.blankToDefault(relKind, "r")) {
            case "p" -> "PARTITIONED TABLE";
            case "f" -> "FOREIGN TABLE";
            default -> "TABLE";
        };
    }

    protected String buildTableKey(String schema, String tableName) {
        return StrUtil.format("{}.{}", schema, tableName);
    }

    protected List<String> splitColumns(String columnNames) {
        if (StrUtil.isBlank(columnNames)) {
            return List.of();
        }
        return StrUtil.splitTrim(columnNames, ',');
    }

    @Override
    protected String resolveJavaType(ColumnInfo columnInfo) {
        return JdbcJavaTypeResolver.resolvePgLike(columnInfo);
    }
}


