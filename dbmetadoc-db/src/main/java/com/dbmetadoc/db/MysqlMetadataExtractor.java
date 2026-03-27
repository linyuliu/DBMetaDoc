package com.dbmetadoc.db;

import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.TableInfo;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysqlMetadataExtractor extends AbstractMetadataExtractor {

    @Override
    protected String getSchemaName(Connection connection, String database) throws SQLException {
        return database;
    }

    @Override
    public DatabaseInfo extract(Connection connection, String database) throws SQLException {
        DatabaseInfo databaseInfo = super.extract(connection, database);
        databaseInfo.setType("MYSQL");

        // Enrich with MySQL-specific comments from information_schema
        enrichTableComments(connection, database, databaseInfo.getTables());
        enrichColumnComments(connection, database, databaseInfo.getTables());

        return databaseInfo;
    }

    private void enrichTableComments(Connection connection, String database, List<TableInfo> tables)
            throws SQLException {
        Map<String, String> commentMap = new HashMap<>();
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    commentMap.put(rs.getString("TABLE_NAME"), rs.getString("TABLE_COMMENT"));
                }
            }
        }

        for (TableInfo table : tables) {
            String comment = commentMap.get(table.getName());
            if (comment != null && !comment.isEmpty()) {
                table.setComment(comment);
            }
        }
    }

    private void enrichColumnComments(Connection connection, String database, List<TableInfo> tables)
            throws SQLException {
        String sql = "SELECT COLUMN_NAME, COLUMN_COMMENT FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

        for (TableInfo table : tables) {
            Map<String, String> commentMap = new HashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, database);
                ps.setString(2, table.getName());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        commentMap.put(rs.getString("COLUMN_NAME"), rs.getString("COLUMN_COMMENT"));
                    }
                }
            }

            if (table.getColumns() != null) {
                for (ColumnInfo col : table.getColumns()) {
                    String comment = commentMap.get(col.getName());
                    if (comment != null && !comment.isEmpty()) {
                        col.setComment(comment);
                    }
                }
            }
        }
    }
}
