package com.dbmetadoc.db.mysql;

import com.dbmetadoc.db.core.AbstractMysqlLikeTableExtractor;
import com.dbmetadoc.db.core.DatabaseType;

/**
 * MySQL 元数据提取实现。
 */
public class MysqlMetadataExtractor extends AbstractMysqlLikeTableExtractor {

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MYSQL;
    }
}
