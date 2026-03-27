package com.dbmetadoc.db.postgresql;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.TableInfo;
import com.dbmetadoc.db.core.AbstractJdbcMetadataExtractor;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * PostgreSQL 元数据提取实现。
 */
public class PostgresqlMetadataExtractor extends AbstractJdbcMetadataExtractor {

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    protected String resolveCatalog(DatabaseConnectionInfo connectionInfo) {
        return connectionInfo.getDatabase();
    }

    @Override
    protected String resolveSchemaPattern(Connection connection, DatabaseConnectionInfo connectionInfo) {
        return StrUtil.blankToDefault(connectionInfo.getSchema(), "public");
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

    @Override
    protected void enrichTableComments(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        String sql = "SELECT c.relname AS table_name, d.description "
                + "FROM pg_class c "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0 "
                + "WHERE c.relkind IN ('r', 'p') AND n.nspname = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, resolveSchemaPattern(connection, connectionInfo));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    applyTableComment(tableMap, rs.getString("table_name"), rs.getString("description"));
                }
            }
        }
    }

    @Override
    protected void enrichColumnComments(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        String sql = "SELECT c.relname AS table_name, a.attname AS column_name, d.description "
                + "FROM pg_class c "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped "
                + "LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = a.attnum "
                + "WHERE c.relkind IN ('r', 'p') AND n.nspname = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, resolveSchemaPattern(connection, connectionInfo));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    applyColumnComment(
                            tableMap,
                            rs.getString("table_name"),
                            rs.getString("column_name"),
                            rs.getString("description"));
                }
            }
        }
    }
}
