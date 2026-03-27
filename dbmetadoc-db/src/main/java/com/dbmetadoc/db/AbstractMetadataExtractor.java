package com.dbmetadoc.db;

import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public abstract class AbstractMetadataExtractor implements MetadataExtractor {

    @Override
    public DatabaseInfo extract(Connection connection, String database) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        DatabaseInfo databaseInfo = new DatabaseInfo();
        databaseInfo.setName(database);
        databaseInfo.setVersion(metaData.getDatabaseProductVersion());

        String schemaName = getSchemaName(connection, database);
        List<TableInfo> tables = extractTables(connection, metaData, schemaName, database);
        databaseInfo.setTables(tables);

        return databaseInfo;
    }

    protected String getSchemaName(Connection connection, String database) throws SQLException {
        return database;
    }

    protected List<TableInfo> extractTables(Connection connection, DatabaseMetaData metaData,
                                             String schemaName, String database) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();

        try (ResultSet rs = metaData.getTables(null, schemaName, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                TableInfo table = new TableInfo();
                table.setName(rs.getString("TABLE_NAME"));
                table.setComment(rs.getString("REMARKS"));
                table.setSchema(schemaName);

                String tableName = table.getName();
                table.setColumns(extractColumns(metaData, schemaName, tableName));
                table.setIndexes(extractIndexes(metaData, schemaName, tableName));
                table.setForeignKeys(extractForeignKeys(metaData, schemaName, tableName));

                tables.add(table);
            }
        }

        return tables;
    }

    protected List<ColumnInfo> extractColumns(DatabaseMetaData metaData, String schemaName, String tableName)
            throws SQLException {
        Set<String> pkColumns = getPrimaryKeyColumns(metaData, schemaName, tableName);
        List<ColumnInfo> columns = new ArrayList<>();

        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, "%")) {
            while (rs.next()) {
                ColumnInfo col = new ColumnInfo();
                col.setName(rs.getString("COLUMN_NAME"));
                col.setType(rs.getString("TYPE_NAME"));
                col.setLength(rs.getInt("COLUMN_SIZE"));
                col.setPrecision(rs.getInt("COLUMN_SIZE"));
                col.setScale(rs.getInt("DECIMAL_DIGITS"));
                col.setNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                col.setDefaultValue(rs.getString("COLUMN_DEF"));
                col.setComment(rs.getString("REMARKS"));
                col.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                col.setPrimaryKey(pkColumns.contains(col.getName()));
                columns.add(col);
            }
        }

        return columns;
    }

    private Set<String> getPrimaryKeyColumns(DatabaseMetaData metaData, String schemaName, String tableName)
            throws SQLException {
        Set<String> pkColumns = new HashSet<>();
        try (ResultSet rs = metaData.getPrimaryKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                pkColumns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return pkColumns;
    }

    protected List<IndexInfo> extractIndexes(DatabaseMetaData metaData, String schemaName, String tableName)
            throws SQLException {
        Map<String, IndexInfo> indexMap = new LinkedHashMap<>();

        try (ResultSet rs = metaData.getIndexInfo(null, schemaName, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue;

                IndexInfo idx = indexMap.computeIfAbsent(indexName, n -> {
                    IndexInfo i = new IndexInfo();
                    i.setName(n);
                    i.setTableName(tableName);
                    i.setColumnNames(new ArrayList<>());
                    return i;
                });

                idx.setUnique(!rs.getBoolean("NON_UNIQUE"));
                String colName = rs.getString("COLUMN_NAME");
                if (colName != null) {
                    idx.getColumnNames().add(colName);
                }
                String indexType = rs.getString("TYPE") != null
                        ? mapIndexType(rs.getShort("TYPE"))
                        : "";
                idx.setType(indexType);
            }
        }

        return new ArrayList<>(indexMap.values());
    }

    private String mapIndexType(short type) {
        switch (type) {
            case DatabaseMetaData.tableIndexStatistic: return "STATISTIC";
            case DatabaseMetaData.tableIndexClustered: return "CLUSTERED";
            case DatabaseMetaData.tableIndexHashed: return "HASH";
            case DatabaseMetaData.tableIndexOther: return "BTREE";
            default: return "BTREE";
        }
    }

    protected List<ForeignKeyInfo> extractForeignKeys(DatabaseMetaData metaData, String schemaName, String tableName)
            throws SQLException {
        List<ForeignKeyInfo> foreignKeys = new ArrayList<>();

        try (ResultSet rs = metaData.getImportedKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                ForeignKeyInfo fk = new ForeignKeyInfo();
                fk.setName(rs.getString("FK_NAME"));
                fk.setTableName(tableName);
                fk.setColumnName(rs.getString("FKCOLUMN_NAME"));
                fk.setReferencedTable(rs.getString("PKTABLE_NAME"));
                fk.setReferencedColumn(rs.getString("PKCOLUMN_NAME"));
                foreignKeys.add(fk);
            }
        }

        return foreignKeys;
    }
}
