package com.dbmetadoc.app.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 目标数据库临时连接池配置。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@ConfigurationProperties(prefix = "dbmetadoc.target-datasource.pool")
public class TargetConnectionPoolProperties {

    private int minimumIdle = 0;

    private int maximumPoolSize = 4;

    private long connectionTimeoutMs = 10_000L;

    private long validationTimeoutMs = 5_000L;

    private long idleTimeoutMs = 30_000L;

    private long maxLifetimeMs = 120_000L;
}


