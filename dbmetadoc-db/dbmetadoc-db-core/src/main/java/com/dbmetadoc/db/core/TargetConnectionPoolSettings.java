package com.dbmetadoc.db.core;

import lombok.Builder;
import lombok.Value;

/**
 * 目标数据库连接池参数。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Value
@Builder
public class TargetConnectionPoolSettings {

    int minimumIdle;

    int maximumPoolSize;

    long connectionTimeoutMs;

    long validationTimeoutMs;

    long idleTimeoutMs;

    long maxLifetimeMs;
}


