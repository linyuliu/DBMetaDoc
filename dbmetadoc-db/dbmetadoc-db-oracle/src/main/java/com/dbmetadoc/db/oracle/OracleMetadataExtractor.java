package com.dbmetadoc.db.oracle;

import com.dbmetadoc.db.core.AbstractOracleLikeTableExtractor;
import com.dbmetadoc.db.core.DatabaseType;

/**
 * Oracle 元数据提取实现。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class OracleMetadataExtractor extends AbstractOracleLikeTableExtractor {

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.ORACLE;
    }
}


