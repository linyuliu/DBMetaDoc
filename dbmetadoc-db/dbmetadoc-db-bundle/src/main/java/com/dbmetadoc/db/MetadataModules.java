package com.dbmetadoc.db;

import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import com.dbmetadoc.db.dameng.DamengMetadataExtractor;
import com.dbmetadoc.db.kingbase.KingbaseMetadataExtractor;
import com.dbmetadoc.db.mysql.MysqlMetadataExtractor;
import com.dbmetadoc.db.oracle.OracleMetadataExtractor;
import com.dbmetadoc.db.postgresql.PostgresqlMetadataExtractor;

import java.util.List;

/**
 * 数据库实现模块装配入口。
 *
 * @author mumu
 * @date 2026-03-30
 */
public final class MetadataModules {

    private MetadataModules() {
    }

    /**
     * 创建统一的元数据注册表。
     */
    public static MetadataExtractorRegistry createRegistry() {
        return new MetadataExtractorRegistry(List.of(
                new MysqlMetadataExtractor(),
                new PostgresqlMetadataExtractor(),
                new OracleMetadataExtractor(),
                new KingbaseMetadataExtractor(),
                new DamengMetadataExtractor()));
    }
}


