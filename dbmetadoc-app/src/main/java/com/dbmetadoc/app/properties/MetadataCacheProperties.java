package com.dbmetadoc.app.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * 元数据缓存配置属性。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@ConfigurationProperties(prefix = "dbmetadoc.cache")
public class MetadataCacheProperties {

    private long ttlHours = 12;
}
