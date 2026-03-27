package com.dbmetadoc.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {

    public static Connection create(DbType dbType, String host, int port, String database,
                                    String username, String password) throws SQLException {
        String url = buildJdbcUrl(dbType, host, port, database);
        return DriverManager.getConnection(url, username, password);
    }

    public static String buildJdbcUrl(DbType dbType, String host, int port, String database) {
        switch (dbType) {
            case MYSQL:
                // useSSL=false is commonly needed for local/dev MySQL instances;
                // enable SSL in production by setting useSSL=true and providing certificates.
                return String.format(
                        "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        host, port, database);
            case POSTGRESQL:
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case KINGBASE:
                return String.format("jdbc:kingbase8://%s:%d/%s", host, port, database);
            default:
                throw new IllegalArgumentException("Unsupported db type: " + dbType);
        }
    }
}
