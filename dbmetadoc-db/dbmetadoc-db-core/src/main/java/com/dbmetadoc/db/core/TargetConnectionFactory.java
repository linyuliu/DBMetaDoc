package com.dbmetadoc.db.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 目标数据库连接工厂。
 */
public final class TargetConnectionFactory {

    private TargetConnectionFactory() {
    }

    /**
     * 创建目标数据库连接。
     */
    public static Connection create(DatabaseConnectionInfo connectionInfo) throws SQLException {
        try {
            Class.forName(connectionInfo.getType().getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC驱动未找到: " + connectionInfo.getType().getDriverClass(), e);
        }
        return DriverManager.getConnection(
                buildJdbcUrl(connectionInfo),
                connectionInfo.getUsername(),
                connectionInfo.getPassword());
    }

    /**
     * 构造目标数据库 JDBC URL。
     */
    public static String buildJdbcUrl(DatabaseConnectionInfo connectionInfo) {
        return connectionInfo.getType().buildJdbcUrl(
                connectionInfo.getHost(),
                connectionInfo.getEffectivePort(),
                connectionInfo.getDatabase(),
                connectionInfo.getSchema());
    }
}
