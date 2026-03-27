package com.dbmetadoc.db.kingbase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;
import com.dbmetadoc.db.core.AbstractJdbcMetadataExtractor;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.DriverDescriptor;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kingbase 元数据提取实现。
 * <p>
 * 参考本地 woodlin 的处理方式，先检测金仓兼容模式，再统一回退到 pg_catalog 做元数据提取。
 * 这样可以规避 MySQL 模式下 JDBC Metadata 返回不完整的问题。
 * </p>
 */
public class KingbaseMetadataExtractor extends AbstractJdbcMetadataExtractor {

    @Getter
    private enum CompatibilityMode {
        PG("pg", "PostgreSQL 兼容模式"),
        ORACLE("oracle", "Oracle 兼容模式"),
        MYSQL("mysql", "MySQL 兼容模式"),
        MSSQL("mssql", "SQL Server 兼容模式");

        private final String code;
        private final String description;

        CompatibilityMode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        static CompatibilityMode fromCode(String code) {
            String normalized = code.trim().toLowerCase();
            for (CompatibilityMode mode : values()) {
                if (Objects.equals(mode.code, normalized)) {
                    return mode;
                }
            }
            return PG;
        }
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.KINGBASE;
    }

    @Override
    public DriverDescriptor getDriverDescriptor() {
        DriverDescriptor descriptor = DatabaseType.KINGBASE.toDescriptor();
        descriptor.setTestSql("自动识别兼容模式: PG/MySQL/MSSQL 使用 SELECT 1，Oracle 使用 SELECT 1 FROM DUAL");
        return descriptor;
    }

    @Override
    public String resolveTestSql(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        CompatibilityMode mode = detectCompatibilityMode(connection);
        return mode == CompatibilityMode.ORACLE ? "SELECT 1 FROM DUAL" : "SELECT 1";
    }

    @Override
    public DatabaseInfo extract(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        CompatibilityMode mode = detectCompatibilityMode(connection);
        List<TableInfo> tables = extractTablesByCatalog(connection, connectionInfo);

        DatabaseInfo databaseInfo = DatabaseInfo.builder()
                .name(resolveDatabaseName(connection, connectionInfo))
                .type(getDatabaseType().name())
                .version(appendMode(connection.getMetaData().getDatabaseProductVersion(), mode))
                .driverName(connection.getMetaData().getDriverName())
                .tables(tables)
                .build();
        enrichDatabaseInfo(connection, connectionInfo, databaseInfo);
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

    CompatibilityMode detectCompatibilityMode(Connection connection) throws SQLException {
        String mode = querySingleString(connection, "SHOW database_mode", 1);
        if (StrUtil.isBlank(mode)) {
            mode = querySingleString(connection, "SELECT setting FROM pg_settings WHERE name = 'database_mode'", "setting");
        }
        if (StrUtil.isBlank(mode)) {
            mode = querySingleString(connection, "SELECT current_setting('database_mode')", 1);
        }
		if(StrUtil.isBlank(mode)){
			mode = CompatibilityMode.ORACLE.code;
	    }
        return CompatibilityMode.fromCode(mode);
    }

    private List<TableInfo> extractTablesByCatalog(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        Map<String, TableInfo> tableMap = loadTables(connection, connectionInfo);
        if (tableMap.isEmpty()) {
            return List.of();
        }

        loadColumns(connection, connectionInfo, tableMap);
        loadPrimaryKeys(connection, connectionInfo, tableMap);
        loadIndexes(connection, connectionInfo, tableMap);
        loadForeignKeys(connection, connectionInfo, tableMap);
        return new ArrayList<>(tableMap.values());
    }

    private Map<String, TableInfo> loadTables(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        Map<String, TableInfo> tableMap = new LinkedHashMap<>();
        boolean hasSchemaFilter = StrUtil.isNotBlank(connectionInfo.getSchema());
        String sql = """
                SELECT
                    n.nspname AS table_schema,
                    c.relname AS table_name,
                    COALESCE(d.description, '') AS table_comment,
                    c.relkind AS rel_kind
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0
                WHERE c.relkind IN ('r', 'p', 'f')
                """ + buildSchemaPredicate("n.nspname", hasSchemaFilter) + """
                ORDER BY n.nspname, c.relname
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setQueryTimeout(30);
            bindSchemaFilter(ps, connectionInfo, hasSchemaFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = TableInfo.builder()
                            .name(rs.getString("table_name"))
                            .comment(StrUtil.emptyToDefault(rs.getString("table_comment"), null))
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

    private void loadColumns(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        boolean hasSchemaFilter = StrUtil.isNotBlank(connectionInfo.getSchema());
        String sql = """
                SELECT
                    n.nspname AS table_schema,
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
                    a.attidentity AS identity_flag
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
                """ + buildSchemaPredicate("n.nspname", hasSchemaFilter) + """
                ORDER BY n.nspname, c.relname, a.attnum
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setQueryTimeout(30);
            bindSchemaFilter(ps, connectionInfo, hasSchemaFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = tableMap.get(buildTableKey(rs.getString("table_schema"), rs.getString("table_name")));
                    if (tableInfo == null) {
                        continue;
                    }
                    String defaultValue = rs.getString("default_value");
                    tableInfo.getColumns().add(ColumnInfo.builder()
                            .name(rs.getString("column_name"))
                            .type(rs.getString("data_type"))
                            .length(getInteger(rs, "char_length"))
                            .precision(getInteger(rs, "numeric_precision"))
                            .scale(getInteger(rs, "numeric_scale"))
                            .nullable(rs.getBoolean("is_nullable"))
                            .primaryKey(Boolean.FALSE)
                            .autoIncrement(isAutoIncrement(defaultValue, rs.getString("identity_flag")))
                            .defaultValue(defaultValue)
                            .comment(StrUtil.emptyToDefault(rs.getString("column_comment"), null))
                            .ordinalPosition(getInteger(rs, "ordinal_position"))
                            .build());
                }
            }
        }
    }

    private void loadPrimaryKeys(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        boolean hasSchemaFilter = StrUtil.isNotBlank(connectionInfo.getSchema());
        String sql = """
                SELECT
                    n.nspname AS table_schema,
                    c.relname AS table_name,
                    a.attname AS column_name
                FROM pg_constraint con
                JOIN pg_class c ON c.oid = con.conrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN LATERAL unnest(con.conkey) WITH ORDINALITY AS cols(attnum, ordinality) ON TRUE
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = cols.attnum
                WHERE con.contype = 'p'
                """ + buildSchemaPredicate("n.nspname", hasSchemaFilter) + """
                ORDER BY n.nspname, c.relname, cols.ordinality
                """;

        Map<String, List<String>> primaryKeyMap = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setQueryTimeout(30);
            bindSchemaFilter(ps, connectionInfo, hasSchemaFilter);
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

    private void loadIndexes(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        boolean hasSchemaFilter = StrUtil.isNotBlank(connectionInfo.getSchema());
        String sql = """
                SELECT
                    n.nspname AS table_schema,
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
                """ + buildSchemaPredicate("n.nspname", hasSchemaFilter) + """
                GROUP BY n.nspname, c.relname, idx_cls.relname, idx.indisunique, am.amname
                ORDER BY n.nspname, c.relname, idx_cls.relname
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setQueryTimeout(30);
            bindSchemaFilter(ps, connectionInfo, hasSchemaFilter);
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

    private void loadForeignKeys(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        boolean hasSchemaFilter = StrUtil.isNotBlank(connectionInfo.getSchema());
        String sql = """
                SELECT
                    con.conname AS fk_name,
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
                """ + buildSchemaPredicate("n.nspname", hasSchemaFilter) + """
                ORDER BY n.nspname, c.relname, con.conname, cols.ordinality
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setQueryTimeout(30);
            bindSchemaFilter(ps, connectionInfo, hasSchemaFilter);
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

    private String buildSchemaPredicate(String columnName, boolean hasSchemaFilter) {
        if (hasSchemaFilter) {
            return " AND " + columnName + " = ? ";
        }
        return " AND " + columnName + " NOT IN ('pg_catalog', 'information_schema', 'pg_toast', 'sys') "
                + "AND " + columnName + " NOT LIKE 'pg_temp_%' "
                + "AND " + columnName + " NOT LIKE 'pg_toast_temp_%' ";
    }

    private void bindSchemaFilter(PreparedStatement ps, DatabaseConnectionInfo connectionInfo, boolean hasSchemaFilter) throws SQLException {
        if (hasSchemaFilter) {
            ps.setString(1, connectionInfo.getSchema());
        }
    }

    private String querySingleString(Connection connection, String sql, int index) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(index);
            }
            return null;
        } catch (SQLException ex) {
            return null;
        }
    }

    private String querySingleString(Connection connection, String sql, String columnName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(columnName);
            }
            return null;
        } catch (SQLException ex) {
            return null;
        }
    }

    private boolean isAutoIncrement(String defaultValue, String identityFlag) {
        return StrUtil.isNotBlank(identityFlag)
                || StrUtil.containsIgnoreCase(defaultValue, "nextval")
                || StrUtil.containsIgnoreCase(defaultValue, "seq_");
    }

    private String appendMode(String version, CompatibilityMode mode) {
        return StrUtil.format("{} [{}]", StrUtil.blankToDefault(version, "unknown"), mode.getDescription());
    }

    private String buildTableKey(String schema, String tableName) {
        return StrUtil.format("{}.{}", schema, tableName);
    }

    private String mapTableType(String relKind) {
        return switch (StrUtil.blankToDefault(relKind, "r")) {
            case "p" -> "PARTITIONED TABLE";
            case "f" -> "FOREIGN TABLE";
            default -> "TABLE";
        };
    }

    private List<String> splitColumns(String columnNames) {
        if (StrUtil.isBlank(columnNames)) {
            return List.of();
        }
        return StrUtil.splitTrim(columnNames, ',');
    }
}
