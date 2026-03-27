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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Oracle-like 表结构提取基类。
 */
@Slf4j
public abstract class AbstractOracleLikeTableExtractor extends AbstractJdbcMetadataExtractor {

    @Override
    protected String resolveSchemaPattern(Connection connection, DatabaseConnectionInfo connectionInfo) {
        return resolveOwner(connectionInfo);
    }

    @Override
    protected String resolveTableSchema(DatabaseConnectionInfo connectionInfo, String catalog, String schemaPattern) {
        return resolveOwner(connectionInfo);
    }

    @Override
    protected String normalizeIdentifier(String identifier) {
        return identifier == null ? null : identifier.toUpperCase();
    }

    @Override
    public DatabaseInfo extract(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        long startTime = System.currentTimeMillis();
        try {
            DatabaseInfo databaseInfo = extractByDictionary(connection, connectionInfo, true);
            log.info("使用 ALL_* 视图完成 {} 元数据提取，Schema：{}，表数量：{}，耗时：{} ms",
                    getDatabaseType().name(), resolveOwner(connectionInfo),
                    databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size(),
                    System.currentTimeMillis() - startTime);
            return databaseInfo;
        } catch (SQLException ex) {
            log.warn("ALL_* 视图提取失败，开始尝试 USER_* 视图，数据库类型：{}，Schema：{}，原因：{}",
                    getDatabaseType().name(), resolveOwner(connectionInfo), ex.getMessage());
        }
        try {
            DatabaseInfo databaseInfo = extractByDictionary(connection, connectionInfo, false);
            log.info("使用 USER_* 视图完成 {} 元数据提取，Schema：{}，表数量：{}，耗时：{} ms",
                    getDatabaseType().name(), resolveOwner(connectionInfo),
                    databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size(),
                    System.currentTimeMillis() - startTime);
            return databaseInfo;
        } catch (SQLException ex) {
            log.warn("USER_* 视图提取失败，回退 JDBC Metadata，数据库类型：{}，原因：{}",
                    getDatabaseType().name(), ex.getMessage());
        }
        return super.extract(connection, connectionInfo);
    }

    protected DatabaseInfo extractByDictionary(Connection connection, DatabaseConnectionInfo connectionInfo, boolean useAllViews)
            throws SQLException {
        Map<String, TableInfo> tableMap = loadTables(connection, connectionInfo, useAllViews);
        loadColumns(connection, connectionInfo, tableMap, useAllViews);
        loadPrimaryKeys(connection, connectionInfo, tableMap, useAllViews);
        loadIndexes(connection, connectionInfo, tableMap, useAllViews);
        loadForeignKeys(connection, connectionInfo, tableMap, useAllViews);

        DatabaseInfo databaseInfo = DatabaseInfo.builder()
                .name(connectionInfo.getDatabase())
                .type(getDatabaseType().name())
                .version(connection.getMetaData().getDatabaseProductVersion())
                .driverName(connection.getMetaData().getDriverName())
                .databaseName(connectionInfo.getDatabase())
                .schemaName(resolveOwner(connectionInfo))
                .catalogName(connectionInfo.getCatalogName())
                .tables(new ArrayList<>(tableMap.values()))
                .build();
        enrichDatabaseInfo(connection, connectionInfo, databaseInfo);
        return databaseInfo;
    }

    @Override
    protected void enrichDatabaseInfo(Connection connection, DatabaseConnectionInfo connectionInfo, DatabaseInfo databaseInfo)
            throws SQLException {
        databaseInfo.setCharset(querySingleValue(connection,
                "SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_CHARACTERSET'"));
        databaseInfo.setCollation(querySingleValue(connection,
                "SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_SORT'"));
    }

    protected Map<String, TableInfo> loadTables(Connection connection, DatabaseConnectionInfo connectionInfo, boolean useAllViews)
            throws SQLException {
        Map<String, TableInfo> tableMap = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(tableSql(useAllViews))) {
            bindOwner(ps, connectionInfo, useAllViews);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    tableMap.put(normalizeIdentifier(tableName), TableInfo.builder()
                            .name(tableName)
                            .comment(StrUtil.trimToNull(rs.getString("COMMENTS")))
                            .schema(resolveOwner(connectionInfo))
                            .tableType("TABLE")
                            .columns(new ArrayList<>())
                            .indexes(new ArrayList<>())
                            .foreignKeys(new ArrayList<>())
                            .build());
                }
            }
        }
        return tableMap;
    }

    protected void loadColumns(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap,
                               boolean useAllViews) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(columnSql(useAllViews))) {
            bindOwner(ps, connectionInfo, useAllViews);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = tableMap.get(normalizeIdentifier(rs.getString("TABLE_NAME")));
                    if (tableInfo == null) {
                        continue;
                    }
                    tableInfo.getColumns().add(ColumnInfo.builder()
                            .name(rs.getString("COLUMN_NAME"))
                            .type(rs.getString("DATA_TYPE"))
                            .length(getInteger(rs, "DATA_LENGTH"))
                            .precision(getInteger(rs, "DATA_PRECISION"))
                            .scale(getInteger(rs, "DATA_SCALE"))
                            .nullable("Y".equalsIgnoreCase(rs.getString("NULLABLE")))
                            .primaryKey(Boolean.FALSE)
                            .autoIncrement("YES".equalsIgnoreCase(getString(rs, "IDENTITY_COLUMN")))
                            .generated("YES".equalsIgnoreCase(getString(rs, "VIRTUAL_COLUMN")))
                            .defaultValue(readClobCompatible(rs, "DATA_DEFAULT"))
                            .comment(StrUtil.trimToNull(rs.getString("COMMENTS")))
                            .ordinalPosition(getInteger(rs, "COLUMN_ID"))
                            .rawType(rs.getString("DATA_TYPE"))
                            .build());
                }
            }
        }
    }

    protected void loadPrimaryKeys(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap,
                                   boolean useAllViews) throws SQLException {
        Map<String, List<String>> primaryKeyMap = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(primaryKeySql(useAllViews))) {
            bindOwner(ps, connectionInfo, useAllViews);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    primaryKeyMap.computeIfAbsent(normalizeIdentifier(tableName), key -> new ArrayList<>())
                            .add(rs.getString("COLUMN_NAME"));
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

    protected void loadIndexes(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap,
                               boolean useAllViews) throws SQLException {
        Map<String, IndexAccumulator> indexMap = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(indexSql(useAllViews))) {
            bindOwner(ps, connectionInfo, useAllViews);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String indexName = rs.getString("INDEX_NAME");
                    if (StrUtil.isBlank(tableName) || StrUtil.isBlank(indexName)) {
                        continue;
                    }
                    String key = tableName + "::" + indexName;
                    boolean unique = !"NONUNIQUE".equalsIgnoreCase(rs.getString("UNIQUENESS"));
                    String indexType = StrUtil.blankToDefault(rs.getString("INDEX_TYPE"), "NORMAL");
                    indexMap.computeIfAbsent(key, value -> new IndexAccumulator(tableName, indexName,
                                    unique,
                                    indexType))
                            .columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        indexMap.values().forEach(accumulator -> {
            TableInfo tableInfo = tableMap.get(normalizeIdentifier(accumulator.tableName));
            if (tableInfo != null) {
                tableInfo.getIndexes().add(IndexInfo.builder()
                        .name(accumulator.indexName)
                        .tableName(accumulator.tableName)
                        .unique(accumulator.unique)
                        .type(accumulator.type)
                        .columnNames(accumulator.columns)
                        .build());
            }
        });
    }

    protected void loadForeignKeys(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap,
                                   boolean useAllViews) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(foreignKeySql(useAllViews))) {
            bindOwner(ps, connectionInfo, useAllViews);
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

    protected String tableSql(boolean useAllViews) {
        if (useAllViews) {
            return "SELECT TABLE_NAME, COMMENTS FROM ALL_TAB_COMMENTS WHERE OWNER = ? AND TABLE_TYPE = 'TABLE' ORDER BY TABLE_NAME";
        }
        return "SELECT TABLE_NAME, COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_TYPE = 'TABLE' ORDER BY TABLE_NAME";
    }

    protected String columnSql(boolean useAllViews) {
        if (useAllViews) {
            return """
                    SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE,
                           c.NULLABLE, c.DATA_DEFAULT, c.COLUMN_ID, c.IDENTITY_COLUMN, c.VIRTUAL_COLUMN, cm.COMMENTS
                    FROM ALL_TAB_COLUMNS c
                    LEFT JOIN ALL_COL_COMMENTS cm
                      ON cm.OWNER = c.OWNER
                     AND cm.TABLE_NAME = c.TABLE_NAME
                     AND cm.COLUMN_NAME = c.COLUMN_NAME
                    WHERE c.OWNER = ?
                    ORDER BY c.TABLE_NAME, c.COLUMN_ID
                    """;
        }
        return """
                SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE,
                       c.NULLABLE, c.DATA_DEFAULT, c.COLUMN_ID, c.IDENTITY_COLUMN, c.VIRTUAL_COLUMN, cm.COMMENTS
                FROM USER_TAB_COLUMNS c
                LEFT JOIN USER_COL_COMMENTS cm
                  ON cm.TABLE_NAME = c.TABLE_NAME
                 AND cm.COLUMN_NAME = c.COLUMN_NAME
                ORDER BY c.TABLE_NAME, c.COLUMN_ID
                """;
    }

    protected String primaryKeySql(boolean useAllViews) {
        if (useAllViews) {
            return """
                    SELECT acc.TABLE_NAME, acc.COLUMN_NAME
                    FROM ALL_CONSTRAINTS ac
                    JOIN ALL_CONS_COLUMNS acc
                      ON acc.OWNER = ac.OWNER
                     AND acc.CONSTRAINT_NAME = ac.CONSTRAINT_NAME
                    WHERE ac.OWNER = ?
                      AND ac.CONSTRAINT_TYPE = 'P'
                    ORDER BY acc.TABLE_NAME, acc.POSITION
                    """;
        }
        return """
                SELECT ucc.TABLE_NAME, ucc.COLUMN_NAME
                FROM USER_CONSTRAINTS uc
                JOIN USER_CONS_COLUMNS ucc
                  ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME
                WHERE uc.CONSTRAINT_TYPE = 'P'
                ORDER BY ucc.TABLE_NAME, ucc.POSITION
                """;
    }

    protected String indexSql(boolean useAllViews) {
        if (useAllViews) {
            return """
                    SELECT ai.TABLE_NAME, ai.INDEX_NAME, ai.UNIQUENESS, ai.INDEX_TYPE, aic.COLUMN_NAME, aic.COLUMN_POSITION
                    FROM ALL_INDEXES ai
                    JOIN ALL_IND_COLUMNS aic
                      ON aic.INDEX_OWNER = ai.OWNER
                     AND aic.INDEX_NAME = ai.INDEX_NAME
                    WHERE ai.TABLE_OWNER = ?
                    ORDER BY ai.TABLE_NAME, ai.INDEX_NAME, aic.COLUMN_POSITION
                    """;
        }
        return """
                SELECT ui.TABLE_NAME, ui.INDEX_NAME, ui.UNIQUENESS, ui.INDEX_TYPE, uic.COLUMN_NAME, uic.COLUMN_POSITION
                FROM USER_INDEXES ui
                JOIN USER_IND_COLUMNS uic
                  ON uic.INDEX_NAME = ui.INDEX_NAME
                ORDER BY ui.TABLE_NAME, ui.INDEX_NAME, uic.COLUMN_POSITION
                """;
    }

    protected String foreignKeySql(boolean useAllViews) {
        if (useAllViews) {
            return """
                    SELECT acc.TABLE_NAME,
                           ac.CONSTRAINT_NAME,
                           acc.COLUMN_NAME,
                           refc.TABLE_NAME AS REFERENCED_TABLE_NAME,
                           refcc.COLUMN_NAME AS REFERENCED_COLUMN_NAME
                    FROM ALL_CONSTRAINTS ac
                    JOIN ALL_CONS_COLUMNS acc
                      ON acc.OWNER = ac.OWNER
                     AND acc.CONSTRAINT_NAME = ac.CONSTRAINT_NAME
                    JOIN ALL_CONSTRAINTS refc
                      ON refc.OWNER = ac.R_OWNER
                     AND refc.CONSTRAINT_NAME = ac.R_CONSTRAINT_NAME
                    JOIN ALL_CONS_COLUMNS refcc
                      ON refcc.OWNER = refc.OWNER
                     AND refcc.CONSTRAINT_NAME = refc.CONSTRAINT_NAME
                     AND refcc.POSITION = acc.POSITION
                    WHERE ac.OWNER = ?
                      AND ac.CONSTRAINT_TYPE = 'R'
                    ORDER BY acc.TABLE_NAME, ac.CONSTRAINT_NAME, acc.POSITION
                    """;
        }
        return """
                SELECT ucc.TABLE_NAME,
                       uc.CONSTRAINT_NAME,
                       ucc.COLUMN_NAME,
                       refc.TABLE_NAME AS REFERENCED_TABLE_NAME,
                       refcc.COLUMN_NAME AS REFERENCED_COLUMN_NAME
                FROM USER_CONSTRAINTS uc
                JOIN USER_CONS_COLUMNS ucc
                  ON ucc.CONSTRAINT_NAME = uc.CONSTRAINT_NAME
                JOIN USER_CONSTRAINTS refc
                  ON refc.CONSTRAINT_NAME = uc.R_CONSTRAINT_NAME
                JOIN USER_CONS_COLUMNS refcc
                  ON refcc.CONSTRAINT_NAME = refc.CONSTRAINT_NAME
                 AND refcc.POSITION = ucc.POSITION
                WHERE uc.CONSTRAINT_TYPE = 'R'
                ORDER BY ucc.TABLE_NAME, uc.CONSTRAINT_NAME, ucc.POSITION
                """;
    }

    protected void bindOwner(PreparedStatement ps, DatabaseConnectionInfo connectionInfo, boolean useAllViews) throws SQLException {
        if (useAllViews) {
            ps.setString(1, resolveOwner(connectionInfo));
        }
    }

    protected String resolveOwner(DatabaseConnectionInfo connectionInfo) {
        return StrUtil.upperFirst(StrUtil.blankToDefault(connectionInfo.getSchema(), connectionInfo.getUsername())).toUpperCase();
    }

    protected String querySingleValue(Connection connection, String sql) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException ex) {
            log.debug("查询 Oracle-like 数据库附加信息失败，SQL：{}，原因：{}", sql, ex.getMessage());
        }
        return null;
    }

    protected String readClobCompatible(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return StrUtil.trimToNull(value == null ? null : value.trim());
    }

    protected static final class IndexAccumulator {
        private final String tableName;
        private final String indexName;
        private final boolean unique;
        private final String type;
        private final List<String> columns = new ArrayList<>();

        private IndexAccumulator(String tableName, String indexName, boolean unique, String type) {
            this.tableName = tableName;
            this.indexName = indexName;
            this.unique = unique;
            this.type = type;
        }
    }
}
