package com.dbmetadoc.db.core;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 JDBC Metadata 的通用提取器基类。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Slf4j
public abstract class AbstractJdbcMetadataExtractor implements MetadataExtractor {

    @Override
    public DatabaseInfo extract(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        long startTime = System.currentTimeMillis();
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = resolveCatalog(connectionInfo);
        String schemaPattern = resolveSchemaPattern(connection, connectionInfo);
        List<TableInfo> tables = extractTables(metaData, catalog, schemaPattern, connectionInfo);
        Map<String, TableInfo> tableMap = toTableMap(tables);
        enrichTableComments(connection, connectionInfo, tableMap);
        enrichColumnComments(connection, connectionInfo, tableMap);

        DatabaseInfo databaseInfo = DatabaseInfo.builder()
                .name(resolveDatabaseName(connection, connectionInfo))
                .type(getDatabaseType().name())
                .version(metaData.getDatabaseProductVersion())
                .driverName(metaData.getDriverName())
                .databaseName(connectionInfo.getDatabase())
                .schemaName(connectionInfo.getSchemaName())
                .catalogName(StrUtil.blankToDefault(connectionInfo.getCatalogName(), catalog))
                .tables(tables)
                .build();
        enrichDatabaseInfo(connection, connectionInfo, databaseInfo);
        log.info("使用 JDBC Metadata 完成元数据提取，数据库类型：{}，数据库：{}，Schema：{}，表数量：{}，耗时：{} ms",
                getDatabaseType().name(), connectionInfo.getDatabase(), connectionInfo.getSchema(),
                tables.size(), System.currentTimeMillis() - startTime);
        return databaseInfo;
    }

    protected String resolveCatalog(DatabaseConnectionInfo connectionInfo) {
        return null;
    }

    protected String resolveSchemaPattern(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        return connectionInfo.getSchema();
    }

    protected String resolveDatabaseName(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        if (StrUtil.isNotBlank(connectionInfo.getDatabase())) {
            return connectionInfo.getDatabase();
        }
        String catalog = connection.getCatalog();
        if (StrUtil.isNotBlank(catalog)) {
            return catalog;
        }
        return connection.getMetaData().getDatabaseProductName();
    }

    protected String resolveTableSchema(DatabaseConnectionInfo connectionInfo, String catalog, String schemaPattern) {
        return StrUtil.blankToDefault(schemaPattern, catalog);
    }

    protected String normalizeIdentifier(String identifier) {
        return identifier;
    }

    protected String[] resolveTableTypes() {
        return new String[]{"TABLE"};
    }

    protected void enrichDatabaseInfo(Connection connection, DatabaseConnectionInfo connectionInfo, DatabaseInfo databaseInfo)
            throws SQLException {
        // default no-op
    }

    protected void enrichTableComments(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        // default no-op
    }

    protected void enrichColumnComments(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        // default no-op
    }

    protected Map<String, TableInfo> toTableMap(List<TableInfo> tables) {
        Map<String, TableInfo> tableMap = new LinkedHashMap<>();
        for (TableInfo table : tables) {
            tableMap.put(normalizeIdentifier(table.getName()), table);
        }
        return tableMap;
    }

    protected List<TableInfo> extractTables(DatabaseMetaData metaData, String catalog, String schemaPattern,
                                            DatabaseConnectionInfo connectionInfo) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(catalog, schemaPattern, "%", resolveTableTypes())) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                Set<String> primaryKeyColumns = getPrimaryKeyColumns(metaData, catalog, schemaPattern, tableName);
                String tableSchema = StrUtil.blankToDefault(getString(rs, "TABLE_SCHEM"),
                        resolveTableSchema(connectionInfo, catalog, schemaPattern));
                tables.add(TableInfo.builder()
                        .name(tableName)
                        .comment(rs.getString("REMARKS"))
                        .schema(tableSchema)
                        .tableType(rs.getString("TABLE_TYPE"))
                        .primaryKey(String.join(",", primaryKeyColumns))
                        .columns(extractColumns(metaData, catalog, schemaPattern, tableName, primaryKeyColumns))
                        .indexes(extractIndexes(metaData, catalog, schemaPattern, tableName))
                        .foreignKeys(extractForeignKeys(metaData, catalog, schemaPattern, tableName))
                        .build());
            }
        }
        return tables;
    }

    protected List<ColumnInfo> extractColumns(DatabaseMetaData metaData, String catalog, String schemaPattern,
                                              String tableName, Set<String> primaryKeyColumns) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(catalog, schemaPattern, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                ColumnInfo columnInfo = ColumnInfo.builder()
                        .name(columnName)
                        .type(rs.getString("TYPE_NAME"))
                        .length(getInteger(rs, "COLUMN_SIZE"))
                        .precision(getInteger(rs, "COLUMN_SIZE"))
                        .scale(getInteger(rs, "DECIMAL_DIGITS"))
                        .nullable("YES".equalsIgnoreCase(getString(rs, "IS_NULLABLE")))
                        .autoIncrement("YES".equalsIgnoreCase(getString(rs, "IS_AUTOINCREMENT")))
                        .generated("YES".equalsIgnoreCase(getString(rs, "IS_GENERATEDCOLUMN")))
                        .primaryKey(primaryKeyColumns.contains(normalizeIdentifier(columnName)))
                        .defaultValue(rs.getString("COLUMN_DEF"))
                        .comment(rs.getString("REMARKS"))
                        .ordinalPosition(getInteger(rs, "ORDINAL_POSITION"))
                        .rawType(rs.getString("TYPE_NAME"))
                        .build();
                columns.add(completeColumn(columnInfo));
            }
        }
        return columns;
    }

    /**
     * 在列对象构造完成后补齐 Java 类型等公共信息。
     */
    protected ColumnInfo completeColumn(ColumnInfo columnInfo) {
        if (columnInfo == null) {
            return null;
        }
        if (StrUtil.isBlank(columnInfo.getJavaType())) {
            columnInfo.setJavaType(resolveJavaType(columnInfo));
        }
        if (StrUtil.isBlank(columnInfo.getJavaType())) {
            columnInfo.setJavaType("String");
        }
        return columnInfo;
    }

    /**
     * 解析列对应的 Java 类型，默认走通用兜底映射。
     */
    protected String resolveJavaType(ColumnInfo columnInfo) {
        return JdbcJavaTypeResolver.resolveGeneric(columnInfo);
    }

    protected List<IndexInfo> extractIndexes(DatabaseMetaData metaData, String catalog, String schemaPattern, String tableName)
            throws SQLException {
        Map<String, IndexInfo> indexMap = new LinkedHashMap<>();
        try (ResultSet rs = metaData.getIndexInfo(catalog, schemaPattern, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (StrUtil.isBlank(indexName) || StrUtil.isBlank(columnName)) {
                    continue;
                }
                IndexInfo indexInfo = indexMap.computeIfAbsent(indexName, key -> IndexInfo.builder()
                        .name(key)
                        .tableName(tableName)
                        .columnNames(new ArrayList<>())
                        .build());
                indexInfo.setUnique(!rs.getBoolean("NON_UNIQUE"));
                indexInfo.getColumnNames().add(columnName);
                indexInfo.setType(mapIndexType(rs.getShort("TYPE")));
            }
        }
        return new ArrayList<>(indexMap.values());
    }

    protected List<ForeignKeyInfo> extractForeignKeys(DatabaseMetaData metaData, String catalog, String schemaPattern,
                                                      String tableName) throws SQLException {
        List<ForeignKeyInfo> foreignKeys = new ArrayList<>();
        try (ResultSet rs = metaData.getImportedKeys(catalog, schemaPattern, tableName)) {
            while (rs.next()) {
                foreignKeys.add(ForeignKeyInfo.builder()
                        .name(rs.getString("FK_NAME"))
                        .tableName(tableName)
                        .columnName(rs.getString("FKCOLUMN_NAME"))
                        .referencedTable(rs.getString("PKTABLE_NAME"))
                        .referencedColumn(rs.getString("PKCOLUMN_NAME"))
                        .build());
            }
        }
        return foreignKeys;
    }

    protected Set<String> getPrimaryKeyColumns(DatabaseMetaData metaData, String catalog, String schemaPattern, String tableName)
            throws SQLException {
        Set<String> primaryKeyColumns = new HashSet<>();
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schemaPattern, tableName)) {
            while (rs.next()) {
                primaryKeyColumns.add(normalizeIdentifier(rs.getString("COLUMN_NAME")));
            }
        }
        return primaryKeyColumns;
    }

    protected void applyTableComment(Map<String, TableInfo> tableMap, String tableName, String comment) {
        TableInfo tableInfo = tableMap.get(normalizeIdentifier(tableName));
        if (tableInfo != null && StrUtil.isNotBlank(comment)) {
            tableInfo.setComment(comment);
        }
    }

    protected void applyTableAttributes(Map<String, TableInfo> tableMap, String tableName, String engine, String charset,
                                        String collation, String rowFormat, String createOptions) {
        TableInfo tableInfo = tableMap.get(normalizeIdentifier(tableName));
        if (tableInfo == null) {
            return;
        }
        if (StrUtil.isNotBlank(engine)) {
            tableInfo.setEngine(engine);
        }
        if (StrUtil.isNotBlank(charset)) {
            tableInfo.setCharset(charset);
        }
        if (StrUtil.isNotBlank(collation)) {
            tableInfo.setCollation(collation);
        }
        if (StrUtil.isNotBlank(rowFormat)) {
            tableInfo.setRowFormat(rowFormat);
        }
        if (StrUtil.isNotBlank(createOptions)) {
            tableInfo.setCreateOptions(createOptions);
        }
    }

    protected void applyColumnComment(Map<String, TableInfo> tableMap, String tableName, String columnName, String comment) {
        if (StrUtil.isBlank(comment)) {
            return;
        }
        TableInfo tableInfo = tableMap.get(normalizeIdentifier(tableName));
        if (tableInfo == null || tableInfo.getColumns() == null) {
            return;
        }
        for (ColumnInfo columnInfo : tableInfo.getColumns()) {
            if (normalizeIdentifier(columnInfo.getName()).equals(normalizeIdentifier(columnName))) {
                columnInfo.setComment(comment);
                return;
            }
        }
    }

    protected Map<String, String> executeSingleValueMap(Connection connection, String sql, List<Object> params,
                                                        String keyColumn, String valueColumn) throws SQLException {
        Map<String, String> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(keyColumn), rs.getString(valueColumn));
                }
            }
        }
        return result;
    }

    protected Integer getInteger(ResultSet rs, String columnLabel) throws SQLException {
        Object value = rs.getObject(columnLabel);
        return value == null ? null : ((Number) value).intValue();
    }

    protected String getString(ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (SQLException e) {
            return null;
        }
    }

    private String mapIndexType(short type) {
        return switch (type) {
            case DatabaseMetaData.tableIndexStatistic -> "STATISTIC";
            case DatabaseMetaData.tableIndexClustered -> "CLUSTERED";
            case DatabaseMetaData.tableIndexHashed -> "HASH";
            case DatabaseMetaData.tableIndexOther -> "BTREE";
            default -> "BTREE";
        };
    }
}


