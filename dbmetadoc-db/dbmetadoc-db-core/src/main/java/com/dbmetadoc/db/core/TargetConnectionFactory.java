package com.dbmetadoc.db.core;

import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * 目标数据库连接工厂。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Slf4j
public final class TargetConnectionFactory {

    private TargetConnectionFactory() {
    }

    /**
     * 基于 HikariCP 创建目标数据库数据源。
     */
    public static HikariDataSource createDataSource(DatabaseConnectionInfo connectionInfo,
                                                    TargetConnectionPoolSettings poolSettings) throws SQLException {
        try {
            Class.forName(connectionInfo.getType().getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC驱动未找到: " + connectionInfo.getType().getDriverClass(), e);
        }
        String jdbcUrl = buildJdbcUrl(connectionInfo);
        HikariConfig config = new HikariConfig();
        config.setPoolName(buildPoolName(connectionInfo));
        config.setDriverClassName(connectionInfo.getType().getDriverClass());
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(connectionInfo.getUsername());
        config.setPassword(connectionInfo.getPassword());
        config.setMinimumIdle(poolSettings.getMinimumIdle());
        config.setMaximumPoolSize(poolSettings.getMaximumPoolSize());
        config.setConnectionTimeout(poolSettings.getConnectionTimeoutMs());
        config.setValidationTimeout(poolSettings.getValidationTimeoutMs());
        config.setIdleTimeout(poolSettings.getIdleTimeoutMs());
        config.setMaxLifetime(poolSettings.getMaxLifetimeMs());
        config.setAutoCommit(true);
        config.setInitializationFailTimeout(1);
        log.info("创建目标库连接池，数据库类型：{}，主机：{}，端口：{}，数据库：{}，JDBC URL：{}",
                connectionInfo.getType().name(),
                connectionInfo.getHost(),
                connectionInfo.getEffectivePort(),
                connectionInfo.getDatabase(),
                jdbcUrl);
        return new HikariDataSource(config);
    }

    /**
     * 构造目标数据库 JDBC URL。
     */
    public static String buildJdbcUrl(DatabaseConnectionInfo connectionInfo) {
        if (StrUtil.isNotBlank(connectionInfo.getResolvedJdbcUrl())) {
            return connectionInfo.getResolvedJdbcUrl();
        }
        return connectionInfo.getType().buildJdbcUrl(connectionInfo);
    }

    private static String buildPoolName(DatabaseConnectionInfo connectionInfo) {
        return StrUtil.format("dbmetadoc-{}-{}-{}-{}",
                connectionInfo.getType().name().toLowerCase(),
                StrUtil.blankToDefault(connectionInfo.getHost(), "unknown"),
                connectionInfo.getEffectivePort(),
                System.nanoTime());
    }
}


