package com.dbmetadoc.db.core;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL-like 表结构提取基类。
 */
@Slf4j
public abstract class AbstractMysqlLikeTableExtractor extends AbstractJdbcMetadataExtractor {

    private static final Pattern TYPE_PATTERN =
            Pattern.compile("^(?<base>[a-zA-Z]+)(?:\\((?<size>[^)]+)\\))?.*$");

    @Override
    protected String resolveCatalog(DatabaseConnectionInfo connectionInfo) {
        return connectionInfo.getDatabase();
    }

    @Override
    protected String resolveSchemaPattern(Connection connection, DatabaseConnectionInfo connectionInfo) {
        return null;
    }

    @Override
    protected String resolveTableSchema(DatabaseConnectionInfo connectionInfo, String catalog, String schemaPattern) {
        return connectionInfo.getDatabase();
    }

    @Override
    public DatabaseInfo extract(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        long startTime = System.currentTimeMillis();
        try {
            log.info("开始使用 SHOW 语句提取 {} 表结构，数据库：{}", getDatabaseType().name(), connectionInfo.getDatabase());
            DatabaseInfo databaseInfo = extractByShow(connection, connectionInfo);
            log.info("SHOW 语句提取完成，数据库：{}，表数量：{}，耗时：{} ms",
                    connectionInfo.getDatabase(),
                    databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size(),
                    System.currentTimeMillis() - startTime);
            return databaseInfo;
        } catch (SQLException ex) {
            log.warn("SHOW 语句提取失败，开始回退 information_schema，数据库：{}，原因：{}",
                    connectionInfo.getDatabase(), ex.getMessage());
        }

        try {
            DatabaseInfo databaseInfo = extractByInformationSchema(connection, connectionInfo);
            log.info("information_schema 提取完成，数据库：{}，表数量：{}，耗时：{} ms",
                    connectionInfo.getDatabase(),
                    databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size(),
                    System.currentTimeMillis() - startTime);
            return databaseInfo;
        } catch (SQLException ex) {
            log.warn("information_schema 提取失败，回退 JDBC Metadata，数据库：{}，原因：{}",
                    connectionInfo.getDatabase(), ex.getMessage());
        }

        return super.extract(connection, connectionInfo);
    }

    protected DatabaseInfo extractByShow(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        Map<String, TableInfo> tableMap = new LinkedHashMap<>();
        String sql = "SHOW TABLE STATUS FROM `" + connectionInfo.getDatabase() + "`";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if ("VIEW".equalsIgnoreCase(rs.getString("Comment"))) {
                    continue;
                }
                String tableName = rs.getString("Name");
                TableInfo tableInfo = TableInfo.builder()
                        .name(tableName)
                        .comment(normalizeComment(rs.getString("Comment")))
                        .schema(connectionInfo.getDatabase())
                        .tableType("TABLE")
                        .engine(rs.getString("Engine"))
                        .collation(rs.getString("Collation"))
                        .charset(resolveCharsetFromCollation(rs.getString("Collation")))
                        .rowFormat(rs.getString("Row_format"))
                        .createOptions(rs.getString("Create_options"))
                        .columns(new ArrayList<>())
                        .indexes(new ArrayList<>())
                        .foreignKeys(new ArrayList<>())
                        .build();
                tableMap.put(normalizeIdentifier(tableName), tableInfo);
            }
        }
        for (TableInfo tableInfo : tableMap.values()) {
            tableInfo.setColumns(loadColumnsByShow(connection, connectionInfo, tableInfo));
            tableInfo.setIndexes(loadIndexesByShow(connection, connectionInfo, tableInfo.getName()));
            tableInfo.setPrimaryKey(resolvePrimaryKey(tableInfo.getColumns()));
        }
        loadForeignKeysFromInformationSchema(connection, connectionInfo, tableMap);
        return buildDatabaseInfo(connection, connectionInfo, new ArrayList<>(tableMap.values()));
    }

    protected DatabaseInfo extractByInformationSchema(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        Map<String, TableInfo> tableMap = new LinkedHashMap<>();
        String tableSql = """
                SELECT TABLE_NAME, TABLE_COMMENT, TABLE_TYPE, ENGINE, TABLE_COLLATION, ROW_FORMAT, CREATE_OPTIONS
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """;
        try (PreparedStatement ps = connection.prepareStatement(tableSql)) {
            ps.setString(1, connectionInfo.getDatabase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = TableInfo.builder()
                            .name(rs.getString("TABLE_NAME"))
                            .comment(normalizeComment(rs.getString("TABLE_COMMENT")))
                            .schema(connectionInfo.getDatabase())
                            .tableType(rs.getString("TABLE_TYPE"))
                            .engine(rs.getString("ENGINE"))
                            .collation(rs.getString("TABLE_COLLATION"))
                            .charset(resolveCharsetFromCollation(rs.getString("TABLE_COLLATION")))
                            .rowFormat(rs.getString("ROW_FORMAT"))
                            .createOptions(rs.getString("CREATE_OPTIONS"))
                            .columns(new ArrayList<>())
                            .indexes(new ArrayList<>())
                            .foreignKeys(new ArrayList<>())
                            .build();
                    tableMap.put(normalizeIdentifier(tableInfo.getName()), tableInfo);
                }
            }
        }
        loadColumnsFromInformationSchema(connection, connectionInfo, tableMap);
        loadIndexesFromInformationSchema(connection, connectionInfo, tableMap);
        loadForeignKeysFromInformationSchema(connection, connectionInfo, tableMap);
        return buildDatabaseInfo(connection, connectionInfo, new ArrayList<>(tableMap.values()));
    }

    protected DatabaseInfo buildDatabaseInfo(Connection connection, DatabaseConnectionInfo connectionInfo, List<TableInfo> tables)
            throws SQLException {
        DatabaseInfo databaseInfo = DatabaseInfo.builder()
                .name(connectionInfo.getDatabase())
                .type(getDatabaseType().name())
                .version(connection.getMetaData().getDatabaseProductVersion())
                .driverName(connection.getMetaData().getDriverName())
                .databaseName(connectionInfo.getDatabase())
                .catalogName(connectionInfo.getDatabase())
                .schemaName(null)
                .tables(tables)
                .build();
        String sql = """
                SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
                FROM information_schema.SCHEMATA
                WHERE SCHEMA_NAME = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, connectionInfo.getDatabase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    databaseInfo.setCharset(rs.getString("DEFAULT_CHARACTER_SET_NAME"));
                    databaseInfo.setCollation(rs.getString("DEFAULT_COLLATION_NAME"));
                }
            }
        }
        return databaseInfo;
    }

    protected List<ColumnInfo> loadColumnsByShow(Connection connection, DatabaseConnectionInfo connectionInfo, TableInfo tableInfo)
            throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        String sql = "SHOW FULL COLUMNS FROM `" + tableInfo.getName() + "` FROM `" + connectionInfo.getDatabase() + "`";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int ordinal = 1;
            while (rs.next()) {
                String type = rs.getString("Type");
                ColumnTypeMeta columnTypeMeta = parseColumnType(type);
                String extra = StrUtil.blankToDefault(rs.getString("Extra"), "");
                ColumnInfo columnInfo = ColumnInfo.builder()
                        .name(rs.getString("Field"))
                        .type(columnTypeMeta.baseType())
                        .length(columnTypeMeta.length())
                        .precision(columnTypeMeta.precision())
                        .scale(columnTypeMeta.scale())
                        .nullable("YES".equalsIgnoreCase(rs.getString("Null")))
                        .primaryKey("PRI".equalsIgnoreCase(rs.getString("Key")))
                        .autoIncrement(StrUtil.containsIgnoreCase(extra, "auto_increment"))
                        .generated(StrUtil.containsIgnoreCase(extra, "generated"))
                        .defaultValue(rs.getString("Default"))
                        .comment(normalizeComment(rs.getString("Comment")))
                        .ordinalPosition(ordinal++)
                        .rawType(type)
                        .build();
                columns.add(completeColumn(columnInfo));
            }
        }
        return columns;
    }

    protected List<IndexInfo> loadIndexesByShow(Connection connection, DatabaseConnectionInfo connectionInfo, String tableName)
            throws SQLException {
        Map<String, IndexInfoBuilder> builders = new LinkedHashMap<>();
        String sql = "SHOW INDEX FROM `" + tableName + "` FROM `" + connectionInfo.getDatabase() + "`";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String indexName = rs.getString("Key_name");
                String columnName = rs.getString("Column_name");
                if (StrUtil.isBlank(indexName) || StrUtil.isBlank(columnName)) {
                    continue;
                }
                boolean unique = !rs.getBoolean("Non_unique");
                String indexType = rs.getString("Index_type");
                IndexInfoBuilder builder = builders.computeIfAbsent(indexName,
                        key -> new IndexInfoBuilder(tableName, key, unique, indexType));
                builder.columns.put(rs.getInt("Seq_in_index"), columnName);
            }
        }
        return builders.values().stream()
                .map(IndexInfoBuilder::build)
                .toList();
    }

    protected void loadColumnsFromInformationSchema(Connection connection, DatabaseConnectionInfo connectionInfo,
                                                    Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION,
                       NUMERIC_SCALE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT, ORDINAL_POSITION, EXTRA, COLUMN_KEY
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ?
                ORDER BY TABLE_NAME, ORDINAL_POSITION
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, connectionInfo.getDatabase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = tableMap.get(normalizeIdentifier(rs.getString("TABLE_NAME")));
                    if (tableInfo == null) {
                        continue;
                    }
                    String extra = StrUtil.blankToDefault(rs.getString("EXTRA"), "");
                    ColumnInfo columnInfo = ColumnInfo.builder()
                            .name(rs.getString("COLUMN_NAME"))
                            .type(rs.getString("DATA_TYPE"))
                            .length(getInteger(rs, "CHARACTER_MAXIMUM_LENGTH"))
                            .precision(getInteger(rs, "NUMERIC_PRECISION"))
                            .scale(getInteger(rs, "NUMERIC_SCALE"))
                            .nullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")))
                            .primaryKey("PRI".equalsIgnoreCase(rs.getString("COLUMN_KEY")))
                            .autoIncrement(StrUtil.containsIgnoreCase(extra, "auto_increment"))
                            .generated(StrUtil.containsIgnoreCase(extra, "generated"))
                            .defaultValue(rs.getString("COLUMN_DEFAULT"))
                            .comment(normalizeComment(rs.getString("COLUMN_COMMENT")))
                            .ordinalPosition(getInteger(rs, "ORDINAL_POSITION"))
                            .rawType(rs.getString("COLUMN_TYPE"))
                            .build();
                    tableInfo.getColumns().add(completeColumn(columnInfo));
                }
            }
        }
        tableMap.values().forEach(tableInfo -> tableInfo.setPrimaryKey(resolvePrimaryKey(tableInfo.getColumns())));
    }

    protected void loadIndexesFromInformationSchema(Connection connection, DatabaseConnectionInfo connectionInfo,
                                                    Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX, INDEX_TYPE
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = ?
                ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX
                """;
        Map<String, IndexInfoBuilder> indexBuilders = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, connectionInfo.getDatabase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    if (StrUtil.isBlank(tableName) || StrUtil.isBlank(indexName) || StrUtil.isBlank(columnName)) {
                        continue;
                    }
                    String key = tableName + "::" + indexName;
                    boolean unique = !rs.getBoolean("NON_UNIQUE");
                    String indexType = rs.getString("INDEX_TYPE");
                    IndexInfoBuilder builder = indexBuilders.computeIfAbsent(key,
                            value -> new IndexInfoBuilder(tableName, indexName, unique, indexType));
                    builder.columns.put(rs.getInt("SEQ_IN_INDEX"), columnName);
                }
            }
        }
        indexBuilders.values().forEach(builder -> {
            TableInfo tableInfo = tableMap.get(normalizeIdentifier(builder.tableName));
            if (tableInfo != null) {
                tableInfo.getIndexes().add(builder.build());
            }
        });
    }

    protected void loadForeignKeysFromInformationSchema(Connection connection, DatabaseConnectionInfo connectionInfo,
                                                        Map<String, TableInfo> tableMap) throws SQLException {
        String sql = """
                SELECT kcu.CONSTRAINT_NAME, kcu.TABLE_NAME, kcu.COLUMN_NAME, kcu.REFERENCED_TABLE_NAME,
                       kcu.REFERENCED_COLUMN_NAME, kcu.ORDINAL_POSITION
                FROM information_schema.KEY_COLUMN_USAGE kcu
                JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
                  ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                 AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                WHERE kcu.TABLE_SCHEMA = ?
                  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
                ORDER BY kcu.TABLE_NAME, kcu.CONSTRAINT_NAME, kcu.ORDINAL_POSITION
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, connectionInfo.getDatabase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = tableMap.get(normalizeIdentifier(rs.getString("TABLE_NAME")));
                    if (tableInfo == null) {
                        continue;
                    }
                    tableInfo.getForeignKeys().add(ForeignKeyInfo.builder()
                            .name(rs.getString("CONSTRAINT_NAME"))
                            .tableName(rs.getString("TABLE_NAME"))
                            .columnName(rs.getString("COLUMN_NAME"))
                            .referencedTable(rs.getString("REFERENCED_TABLE_NAME"))
                            .referencedColumn(rs.getString("REFERENCED_COLUMN_NAME"))
                            .build());
                }
            }
        }
    }

    protected String normalizeComment(String comment) {
        return StrUtil.trimToNull(comment);
    }

    @Override
    protected String resolveJavaType(ColumnInfo columnInfo) {
        return JdbcJavaTypeResolver.resolveMysqlLike(columnInfo);
    }

    protected String resolveCharsetFromCollation(String collation) {
        if (StrUtil.isBlank(collation) || !collation.contains("_")) {
            return null;
        }
        return StrUtil.subBefore(collation, "_", false);
    }

    protected String resolvePrimaryKey(List<ColumnInfo> columns) {
        if (columns == null || columns.isEmpty()) {
            return null;
        }
        List<String> primaryKeys = columns.stream()
                .filter(columnInfo -> Boolean.TRUE.equals(columnInfo.getPrimaryKey()))
                .map(ColumnInfo::getName)
                .toList();
        return primaryKeys.isEmpty() ? null : String.join(",", primaryKeys);
    }

    protected ColumnTypeMeta parseColumnType(String rawType) {
        if (StrUtil.isBlank(rawType)) {
            return new ColumnTypeMeta(null, null, null, null);
        }
        Matcher matcher = TYPE_PATTERN.matcher(rawType);
        if (!matcher.matches()) {
            return new ColumnTypeMeta(rawType, null, null, null);
        }
        String base = matcher.group("base");
        String size = matcher.group("size");
        if (StrUtil.isBlank(size)) {
            return new ColumnTypeMeta(base, null, null, null);
        }
        List<String> numbers = StrUtil.splitTrim(size, ',');
        Integer first = parseNullableInteger(numbers.isEmpty() ? null : numbers.get(0));
        Integer second = parseNullableInteger(numbers.size() > 1 ? numbers.get(1) : null);
        if (StrUtil.containsIgnoreCase(base, "decimal")
                || StrUtil.containsIgnoreCase(base, "numeric")
                || StrUtil.containsIgnoreCase(base, "double")
                || StrUtil.containsIgnoreCase(base, "float")) {
            return new ColumnTypeMeta(base, null, first, second);
        }
        return new ColumnTypeMeta(base, first, first, second);
    }

    private Integer parseNullableInteger(String value) {
        if (StrUtil.isBlank(value) || !StrUtil.isNumeric(value)) {
            return null;
        }
        return Integer.parseInt(value);
    }

    protected record ColumnTypeMeta(String baseType, Integer length, Integer precision, Integer scale) {
    }

    protected static final class IndexInfoBuilder {
        private final String tableName;
        private final String indexName;
        private final boolean unique;
        private final String type;
        private final Map<Integer, String> columns = new TreeMap<>();

        private IndexInfoBuilder(String tableName, String indexName, boolean unique, String type) {
            this.tableName = tableName;
            this.indexName = indexName;
            this.unique = unique;
            this.type = type;
        }

        private IndexInfo build() {
            return IndexInfo.builder()
                    .name(indexName)
                    .tableName(tableName)
                    .unique(unique)
                    .type(type)
                    .columnNames(new ArrayList<>(columns.values()))
                    .build();
        }
    }
}
