package com.dbmetadoc.db;

import com.dbmetadoc.common.model.DatabaseInfo;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * KingBase (人大金仓) is PostgreSQL-compatible, so we reuse the PostgreSQL extractor.
 * Requires the KingBase JDBC driver (kingbase8-*.jar) on the classpath.
 */
public class KingbaseMetadataExtractor extends PostgresqlMetadataExtractor {

    @Override
    public DatabaseInfo extract(Connection connection, String database) throws SQLException {
        DatabaseInfo databaseInfo = super.extract(connection, database);
        databaseInfo.setType("KINGBASE");
        return databaseInfo;
    }
}
