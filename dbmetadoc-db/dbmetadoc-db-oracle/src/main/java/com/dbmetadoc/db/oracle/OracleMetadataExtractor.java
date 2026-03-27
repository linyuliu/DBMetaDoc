package com.dbmetadoc.db.oracle;

import cn.hutool.core.util.StrUtil;
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
 * Oracle 元数据提取实现。
 */
public class OracleMetadataExtractor extends AbstractJdbcMetadataExtractor {

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.ORACLE;
    }

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
    protected void enrichTableComments(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        String sql = "SELECT TABLE_NAME, COMMENTS FROM ALL_TAB_COMMENTS WHERE OWNER = ? AND TABLE_TYPE = 'TABLE'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, resolveOwner(connectionInfo));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    applyTableComment(tableMap, rs.getString("TABLE_NAME"), rs.getString("COMMENTS"));
                }
            }
        }
    }

    @Override
    protected void enrichColumnComments(Connection connection, DatabaseConnectionInfo connectionInfo, Map<String, TableInfo> tableMap)
            throws SQLException {
        String sql = "SELECT TABLE_NAME, COLUMN_NAME, COMMENTS FROM ALL_COL_COMMENTS WHERE OWNER = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, resolveOwner(connectionInfo));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    applyColumnComment(
                            tableMap,
                            rs.getString("TABLE_NAME"),
                            rs.getString("COLUMN_NAME"),
                            rs.getString("COMMENTS"));
                }
            }
        }
    }

    protected String resolveOwner(DatabaseConnectionInfo connectionInfo) {
        return StrUtil.upperFirst(StrUtil.blankToDefault(connectionInfo.getSchema(), connectionInfo.getUsername())).toUpperCase();
    }
}
