package com.dbmetadoc.db;

public class MetadataExtractorFactory {

    public static MetadataExtractor create(DbType dbType) {
        switch (dbType) {
            case MYSQL:
                return new MysqlMetadataExtractor();
            case POSTGRESQL:
                return new PostgresqlMetadataExtractor();
            case KINGBASE:
                return new KingbaseMetadataExtractor();
            default:
                throw new IllegalArgumentException("Unsupported db type: " + dbType);
        }
    }
}
