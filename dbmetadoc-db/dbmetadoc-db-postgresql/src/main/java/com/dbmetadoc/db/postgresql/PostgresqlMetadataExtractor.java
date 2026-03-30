package com.dbmetadoc.db.postgresql;

import com.dbmetadoc.db.core.AbstractPgLikeTableExtractor;
import com.dbmetadoc.db.core.DatabaseType;

/**
 * PostgreSQL 元数据提取实现。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class PostgresqlMetadataExtractor extends AbstractPgLikeTableExtractor {

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRESQL;
    }
}


