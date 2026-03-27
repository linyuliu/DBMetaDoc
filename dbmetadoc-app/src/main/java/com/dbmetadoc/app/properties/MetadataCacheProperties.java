package com.dbmetadoc.app.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "dbmetadoc.cache")
public class MetadataCacheProperties {

    private long ttlHours = 12;
}
