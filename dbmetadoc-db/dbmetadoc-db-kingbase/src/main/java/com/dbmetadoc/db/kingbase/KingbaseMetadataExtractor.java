package com.dbmetadoc.db.kingbase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;
import com.dbmetadoc.db.core.AbstractOracleLikeTableExtractor;
import com.dbmetadoc.db.core.AbstractPgLikeTableExtractor;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.DriverDescriptor;
import com.dbmetadoc.db.core.JdbcJavaTypeResolver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kingbase 元数据提取实现。
 * <p>
 * Kingbase 是 PG 内核，但兼容模式并不等价于“直接复用对应数据库实现”。
 * 尤其 MySQL 模式下，SHOW 语句、字段语义和 JDBC metadata 表现都与原生 MySQL 不完全一致，
 * 所以这里按模式分别组织提取路径，避免直接套用通用 MysqlLike 提取器。
 * </p>
 *
 * @author mumu
 * @date 2026-03-30
 */
@Slf4j
public class KingbaseMetadataExtractor extends AbstractPgLikeTableExtractor {

    private final ThreadLocal<CompatibilityMode> currentMode = ThreadLocal.withInitial(() -> CompatibilityMode.PG);
    private final KingbaseOracleModeExtractor oracleModeExtractor = new KingbaseOracleModeExtractor();

    @Getter
    enum CompatibilityMode {
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
            if (StrUtil.isBlank(code)) {
                return PG;
            }
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
        descriptor.setTestSql("自动识别兼容模式：PG/MySQL/MSSQL 使用 SELECT 1，Oracle 使用 SELECT 1 FROM DUAL");
        descriptor.setMetadataStrategy("模式探测 -> PG 目录 / Oracle 字典 / Kingbase MySQL 信息模式");
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
        currentMode.set(mode);
        try {
            DatabaseConnectionInfo normalizedInfo = normalizeConnectionInfo(connection, connectionInfo, mode);
            log.info("开始提取 Kingbase 元数据，兼容模式：{}，数据库：{}，Schema：{}",
                    mode.getDescription(), normalizedInfo.getDatabase(), normalizedInfo.getSchema());

            DatabaseInfo databaseInfo = switch (mode) {
                case ORACLE -> oracleModeExtractor.extract(connection, normalizedInfo);
                case MYSQL -> extractMysqlMode(connection, normalizedInfo);
                case PG, MSSQL -> super.extract(connection, normalizedInfo);
            };
            databaseInfo.setVersion(appendMode(databaseInfo.getVersion(), mode));
            databaseInfo.setType(DatabaseType.KINGBASE.name());
            databaseInfo.setDatabaseName(normalizedInfo.getDatabase());
            databaseInfo.setCatalogName(StrUtil.blankToDefault(databaseInfo.getCatalogName(), normalizedInfo.getDatabase()));
            databaseInfo.setSchemaName(normalizedInfo.getSchema());
            return databaseInfo;
        } finally {
            currentMode.remove();
        }
    }

    @Override
    protected String resolveJavaType(ColumnInfo columnInfo) {
        CompatibilityMode mode = currentMode.get();
        return JdbcJavaTypeResolver.resolveKingbase(columnInfo, mode == null ? CompatibilityMode.PG.getCode() : mode.getCode());
    }

    CompatibilityMode detectCompatibilityMode(Connection connection) throws SQLException {
        String mode = querySingleString(connection, "SHOW database_mode", 1);
        if (StrUtil.isBlank(mode)) {
            mode = querySingleString(connection, "SELECT setting FROM pg_settings WHERE name = 'database_mode'", "setting");
        }
        if (StrUtil.isBlank(mode)) {
            mode = querySingleString(connection, "SELECT current_setting('database_mode')", 1);
        }
        CompatibilityMode compatibilityMode = CompatibilityMode.fromCode(mode);
        log.info("检测到 Kingbase 兼容模式：{}", compatibilityMode.getDescription());
        return compatibilityMode;
    }

    private DatabaseInfo extractMysqlMode(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        long startTime = System.currentTimeMillis();
        try {
            DatabaseInfo databaseInfo = extractMysqlModeByInformationSchema(connection, connectionInfo);
            log.info("Kingbase MySQL 模式提取完成，数据库：{}，Schema：{}，表数量：{}，耗时：{} ms",
                    connectionInfo.getDatabase(),
                    connectionInfo.getSchema(),
                    databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size(),
                    System.currentTimeMillis() - startTime);
            return databaseInfo;
        } catch (SQLException ex) {
            log.warn("Kingbase MySQL 模式信息模式提取失败，回退 PG 目录查询，数据库：{}，Schema：{}，原因：{}",
                    connectionInfo.getDatabase(), connectionInfo.getSchema(), ex.getMessage());
            return super.extract(connection, connectionInfo);
        }
    }

    private DatabaseInfo extractMysqlModeByInformationSchema(Connection connection, DatabaseConnectionInfo connectionInfo)
            throws SQLException {
        String schema = StrUtil.blankToDefault(connectionInfo.getSchema(), resolveCurrentSchema(connection));
        Map<String, TableInfo> tableMap = loadMysqlModeTables(connection, schema);
        loadMysqlModeColumns(connection, schema, tableMap);
        loadMysqlModePrimaryKeys(connection, schema, tableMap);
        loadMysqlModeIndexes(connection, schema, tableMap);
        loadMysqlModeForeignKeys(connection, schema, tableMap);

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
        return databaseInfo;
    }

    private Map<String, TableInfo> loadMysqlModeTables(Connection connection, String schema) throws SQLException {
        Map<String, TableInfo> tableMap = new LinkedHashMap<>();
        String sql = """
                SELECT t.table_schema,
                       t.table_name,
                       COALESCE(obj_description(c.oid, 'pg_class'), '') AS table_comment,
                       c.relkind AS rel_kind
                FROM information_schema.tables t
                JOIN pg_namespace n ON n.nspname = t.table_schema
                JOIN pg_class c ON c.relname = t.table_name AND c.relnamespace = n.oid
                WHERE t.table_catalog = current_database()
                  AND t.table_schema = ?
                  AND c.relkind IN ('r', 'p', 'f')
                ORDER BY t.table_name
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

    private void loadMysqlModeColumns(Connection connection, String schema, Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT col.table_schema,
                       col.table_name,
                       col.column_name,
                       COALESCE(NULLIF(col.udt_name, ''), col.data_type) AS data_type,
                       format_type(a.atttypid, a.atttypmod) AS raw_type,
                       col.character_maximum_length AS char_length,
                       col.numeric_precision AS numeric_precision,
                       col.numeric_scale AS numeric_scale,
                       col.is_nullable,
                       col.column_default,
                       col.ordinal_position,
                       COALESCE(pg_catalog.col_description(c.oid, a.attnum), '') AS column_comment,
                       a.attidentity AS identity_flag,
                       a.attgenerated AS generated_flag
                FROM information_schema.columns col
                JOIN pg_namespace n ON n.nspname = col.table_schema
                JOIN pg_class c ON c.relname = col.table_name AND c.relnamespace = n.oid
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attname = col.column_name
                WHERE col.table_catalog = current_database()
                  AND col.table_schema = ?
                  AND a.attnum > 0
                  AND NOT a.attisdropped
                ORDER BY col.table_name, col.ordinal_position
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableKey = buildTableKey(rs.getString("table_schema"), rs.getString("table_name"));
                    TableInfo tableInfo = tableMap.get(tableKey);
                    if (tableInfo == null) {
                        continue;
                    }
                    String defaultValue = rs.getString("column_default");
                    ColumnInfo columnInfo = ColumnInfo.builder()
                            .name(rs.getString("column_name"))
                            .type(rs.getString("data_type"))
                            .length(getInteger(rs, "char_length"))
                            .precision(getInteger(rs, "numeric_precision"))
                            .scale(getInteger(rs, "numeric_scale"))
                            .nullable("YES".equalsIgnoreCase(rs.getString("is_nullable")))
                            .primaryKey(Boolean.FALSE)
                            .autoIncrement(isAutoIncrement(defaultValue, rs.getString("identity_flag")))
                            .generated(StrUtil.isNotBlank(rs.getString("generated_flag")))
                            .defaultValue(defaultValue)
                            .comment(StrUtil.trimToNull(rs.getString("column_comment")))
                            .ordinalPosition(getInteger(rs, "ordinal_position"))
                            .rawType(rs.getString("raw_type"))
                            .build();
                    tableInfo.getColumns().add(completeColumn(columnInfo));
                }
            }
        }
    }

    private void loadMysqlModePrimaryKeys(Connection connection, String schema, Map<String, TableInfo> tableMap) throws SQLException {
        Map<String, List<String>> primaryKeyMap = new LinkedHashMap<>();
        String sql = """
                SELECT kcu.table_schema,
                       kcu.table_name,
                       kcu.column_name,
                       kcu.ordinal_position
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_catalog = kcu.constraint_catalog
                 AND tc.constraint_schema = kcu.constraint_schema
                 AND tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema = kcu.table_schema
                 AND tc.table_name = kcu.table_name
                WHERE tc.constraint_type = 'PRIMARY KEY'
                  AND tc.table_schema = ?
                ORDER BY kcu.table_name, kcu.ordinal_position
                """;
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

    private void loadMysqlModeIndexes(Connection connection, String schema, Map<String, TableInfo> tableMap) throws SQLException {
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

    private void loadMysqlModeForeignKeys(Connection connection, String schema, Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT kcu.constraint_name,
                       kcu.table_schema,
                       kcu.table_name,
                       kcu.column_name,
                       ccu.table_name AS referenced_table_name,
                       ccu.column_name AS referenced_column_name,
                       kcu.ordinal_position
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_catalog = kcu.constraint_catalog
                 AND tc.constraint_schema = kcu.constraint_schema
                 AND tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema = kcu.table_schema
                 AND tc.table_name = kcu.table_name
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_catalog = tc.constraint_catalog
                 AND ccu.constraint_schema = tc.constraint_schema
                 AND ccu.constraint_name = tc.constraint_name
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND tc.table_schema = ?
                ORDER BY kcu.table_name, kcu.constraint_name, kcu.ordinal_position
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableKey = buildTableKey(rs.getString("table_schema"), rs.getString("table_name"));
                    TableInfo tableInfo = tableMap.get(tableKey);
                    if (tableInfo == null) {
                        continue;
                    }
                    tableInfo.getForeignKeys().add(ForeignKeyInfo.builder()
                            .name(rs.getString("constraint_name"))
                            .tableName(rs.getString("table_name"))
                            .columnName(rs.getString("column_name"))
                            .referencedTable(rs.getString("referenced_table_name"))
                            .referencedColumn(rs.getString("referenced_column_name"))
                            .build());
                }
            }
        }
    }

    private DatabaseConnectionInfo normalizeConnectionInfo(Connection connection,
                                                           DatabaseConnectionInfo source,
                                                           CompatibilityMode mode) throws SQLException {
        String schema = switch (mode) {
            case ORACLE -> StrUtil.blankToDefault(source.getSchema(), source.getUsername()).toUpperCase();
            case PG, MSSQL, MYSQL -> StrUtil.blankToDefault(source.getSchema(), resolveCurrentSchema(connection));
        };
        return DatabaseConnectionInfo.builder()
                .type(source.getType())
                .jdbcUrl(source.getJdbcUrl())
                .host(source.getHost())
                .port(source.getPort())
                .database(source.getDatabase())
                .schema(schema)
                .username(source.getUsername())
                .password(source.getPassword())
                .catalogName(source.getDatabase())
                .schemaName(schema)
                .serviceNameOrSid(source.getServiceNameOrSid())
                .sidMode(source.isSidMode())
                .jdbcParameters(source.getJdbcParameters())
                .resolvedJdbcUrl(source.getResolvedJdbcUrl())
                .build();
    }

    private String resolveCurrentSchema(Connection connection) {
        String schema = querySingleString(connection, "SELECT current_schema()", 1);
        return StrUtil.blankToDefault(schema, "public");
    }

    private String appendMode(String version, CompatibilityMode mode) {
        return StrUtil.format("{} [{}]", StrUtil.blankToDefault(version, "unknown"), mode.getDescription());
    }

    private String querySingleString(Connection connection, String sql, int index) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(index);
            }
        } catch (SQLException ex) {
            log.debug("执行 Kingbase 兼容模式探测 SQL 失败，SQL：{}，原因：{}", sql, ex.getMessage());
        }
        return null;
    }

    private String querySingleString(Connection connection, String sql, String columnName) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(columnName);
            }
        } catch (SQLException ex) {
            log.debug("执行 Kingbase 兼容模式探测 SQL 失败，SQL：{}，原因：{}", sql, ex.getMessage());
        }
        return null;
    }

    /**
     * Oracle 模式单独沿用字典视图，但仍然复用 Kingbase 自己的类型映射。
     */
    private final class KingbaseOracleModeExtractor extends AbstractOracleLikeTableExtractor {

        @Override
        public DatabaseType getDatabaseType() {
            return DatabaseType.KINGBASE;
        }

        @Override
        protected String resolveJavaType(ColumnInfo columnInfo) {
            return JdbcJavaTypeResolver.resolveKingbase(columnInfo, CompatibilityMode.ORACLE.getCode());
        }
    }
}


