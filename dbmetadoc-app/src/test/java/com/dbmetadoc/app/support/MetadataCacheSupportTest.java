package com.dbmetadoc.app.support;

import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 元数据缓存辅助工具测试。
 *
 * @author mumu
 * @date 2026-03-28
 */
class MetadataCacheSupportTest {

    @Test
    void shouldBuildStableCacheKey() {
        DatabaseConnectionInfo connectionInfo = DatabaseConnectionInfo.builder()
                .type(DatabaseType.MYSQL)
                .host("127.0.0.1")
                .port(3306)
                .database("demo")
                .schema(null)
                .username("root")
                .build();

        String first = MetadataCacheSupport.buildCacheKey(connectionInfo);
        String second = MetadataCacheSupport.buildCacheKey(connectionInfo);

        assertEquals(first, second);
    }

    @Test
    void shouldUseDifferentHashWhenValueChanges() {
        String first = MetadataCacheSupport.buildMetadataHash("{\"name\":\"demo\"}");
        String second = MetadataCacheSupport.buildMetadataHash("{\"name\":\"demo2\"}");

        assertNotEquals(first, second);
    }
}
