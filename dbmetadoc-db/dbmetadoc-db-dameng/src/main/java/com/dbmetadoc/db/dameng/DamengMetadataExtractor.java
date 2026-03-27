package com.dbmetadoc.db.dameng;

import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.oracle.OracleMetadataExtractor;

/**
 * 达梦元数据提取实现。
 * <p>
 * 当前优先复用 Oracle 兼容视图抽取。
 * </p>
 */
public class DamengMetadataExtractor extends OracleMetadataExtractor {

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.DAMENG;
    }
}
