package com.dbmetadoc.db.core;

import com.dbmetadoc.common.model.DatabaseInfo;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 元数据提取器统一接口。
 *
 * @author mumu
 * @date 2026-03-30
 */
public interface MetadataExtractor {

    DatabaseType getDatabaseType();

    default boolean supports(DatabaseType databaseType) {
        return getDatabaseType() == databaseType;
    }

    default DriverDescriptor getDriverDescriptor() {
        return getDatabaseType().toDescriptor();
    }

    /**
     * 解析当前连接应该使用的测试 SQL。
     */
    default String resolveTestSql(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        return getDatabaseType().getTestSql();
    }

    DatabaseInfo extract(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException;
}


