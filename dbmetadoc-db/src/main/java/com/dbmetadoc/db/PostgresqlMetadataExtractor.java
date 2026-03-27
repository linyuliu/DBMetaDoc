package com.dbmetadoc.db;

import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.TableInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresqlMetadataExtractor extends AbstractMetadataExtractor {

    @Override
    protected String getSchemaName(Connection connection, String database) throws SQLException {
        return "public";
    }

    @Override
    public DatabaseInfo extract(Connection connection, String database) throws SQLException {
        DatabaseInfo databaseInfo = super.extract(connection, database);
        databaseInfo.setType("POSTGRESQL");

        enrichTableComments(connection, databaseInfo.getTables());
        enrichColumnComments(connection, databaseInfo.getTables());

        return databaseInfo;
    }

    protected void enrichTableComments(Connection connection, List<TableInfo> tables) throws SQLException {
        String sql = "SELECT c.relname, d.description "
                + "FROM pg_class c "
                + "LEFT JOIN pg_description d ON c.oid = d.objoid AND d.objsubid = 0 "
                + "WHERE c.relkind = 'r' "
                + "AND c.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = current_schema())";

        Map<String, String> commentMap = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String tableName = rs.getString("relname");
                String description = rs.getString("description");
                if (description != null) {
                    commentMap.put(tableName, description);
                }
            }
        }

        for (TableInfo table : tables) {
            String comment = commentMap.get(table.getName());
            if (comment != null) {
                table.setComment(comment);
            }
        }
    }

    protected void enrichColumnComments(Connection connection, List<TableInfo> tables) throws SQLException {
        String sql = "SELECT a.attname, d.description "
                + "FROM pg_class c "
                + "JOIN pg_attribute a ON c.oid = a.attrelid "
                + "LEFT JOIN pg_description d ON c.oid = d.objoid AND d.objsubid = a.attnum "
                + "WHERE c.relname = ? "
                + "AND c.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = current_schema()) "
                + "AND a.attnum > 0 AND NOT a.attisdropped";

        for (TableInfo table : tables) {
            Map<String, String> commentMap = new HashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, table.getName());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String colName = rs.getString("attname");
                        String description = rs.getString("description");
                        if (description != null) {
                            commentMap.put(colName, description);
                        }
                    }
                }
            }

            if (table.getColumns() != null) {
                for (ColumnInfo col : table.getColumns()) {
                    String comment = commentMap.get(col.getName());
                    if (comment != null) {
                        col.setComment(comment);
                    }
                }
            }
        }
    }
}
