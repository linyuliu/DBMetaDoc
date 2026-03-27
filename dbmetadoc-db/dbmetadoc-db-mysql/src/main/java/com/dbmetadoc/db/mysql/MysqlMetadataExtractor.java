package com.dbmetadoc.db.mysql;

import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.TableInfo;
import com.dbmetadoc.db.core.AbstractJdbcMetadataExtractor;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * MySQL 元数据提取实现。
 */
public class MysqlMetadataExtractor extends AbstractJdbcMetadataExtractor {

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MYSQL;
    }

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
    protected void enrichDatabaseInfo(Connection connection, DatabaseConnectionInfo connectionInfo, DatabaseInfo databaseInfo)
            throws SQLException {
        String sql = "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME "
                + "FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, connectionInfo.getDatabase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    databaseInfo.setCharset(rs.getString("DEFAULT_CHARACTER_SET_NAME"));
                    databaseInfo.setCollation(rs.getString("DEFAULT_COLLATION_NAME"));
                }
            }
        }
    }

    @Override
    protected void enrichTableComments(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, connectionInfo.getDatabase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    applyTableComment(tableMap, rs.getString("TABLE_NAME"), rs.getString("TABLE_COMMENT"));
                }
            }
        }
    }

    @Override
    protected void enrichColumnComments(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        String sql = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_COMMENT "
                + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, connectionInfo.getDatabase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    applyColumnComment(
                            tableMap,
                            rs.getString("TABLE_NAME"),
                            rs.getString("COLUMN_NAME"),
                            rs.getString("COLUMN_COMMENT"));
                }
            }
        }
    }
}
