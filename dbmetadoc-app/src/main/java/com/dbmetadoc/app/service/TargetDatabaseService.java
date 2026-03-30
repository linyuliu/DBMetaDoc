package com.dbmetadoc.app.service;

import com.dbmetadoc.app.properties.TargetConnectionPoolProperties;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import com.dbmetadoc.db.core.TargetConnectionFactory;
import com.dbmetadoc.db.core.TargetConnectionPoolSettings;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * 目标数据库操作服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TargetDatabaseService {

    private final MetadataExtractorRegistry metadataExtractorRegistry;
    private final TargetConnectionPoolProperties targetConnectionPoolProperties;
    private final TaskExecutor taskExecutor;

    public void testConnection(DatabaseConnectionInfo connectionInfo) {
        doTestConnection(connectionInfo);
    }

    public CompletableFuture<Void> testConnectionAsync(DatabaseConnectionInfo connectionInfo) {
        return CompletableFuture.runAsync(() -> doTestConnection(connectionInfo), taskExecutor);
    }

    public DatabaseInfo extract(DatabaseConnectionInfo connectionInfo) {
        return doExtract(connectionInfo);
    }

    public CompletableFuture<DatabaseInfo> extractAsync(DatabaseConnectionInfo connectionInfo) {
        return CompletableFuture.supplyAsync(() -> doExtract(connectionInfo), taskExecutor);
    }

    private void doTestConnection(DatabaseConnectionInfo connectionInfo) {
        long startTime = System.currentTimeMillis();
        try (HikariDataSource dataSource = TargetConnectionFactory.createDataSource(connectionInfo, buildPoolSettings());
             Connection connection = dataSource.getConnection()) {
            String testSql = metadataExtractorRegistry.getExtractor(connectionInfo.getType())
                    .resolveTestSql(connection, connectionInfo);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            Boolean success = jdbcTemplate.query(testSql, (ResultSetExtractor<Boolean>) rs -> rs.next());
            if (!Boolean.TRUE.equals(success)) {
                throw new BusinessException(ResultCode.DATASOURCE_TEST_FAILED, "连接测试未返回结果");
            }
            log.info("连接测试通过，数据库类型：{}，主机：{}，端口：{}，数据库：{}，测试SQL：{}，耗时：{} ms",
                    connectionInfo.getType().name(),
                    connectionInfo.getHost(),
                    connectionInfo.getEffectivePort(),
                    connectionInfo.getDatabase(),
                    testSql,
                    System.currentTimeMillis() - startTime);
        } catch (SQLException e) {
            throw new BusinessException(ResultCode.DATASOURCE_TEST_FAILED, "连接测试失败: " + e.getMessage(), e);
        }
    }

    private DatabaseInfo doExtract(DatabaseConnectionInfo connectionInfo) {
        long startTime = System.currentTimeMillis();
        try (HikariDataSource dataSource = TargetConnectionFactory.createDataSource(connectionInfo, buildPoolSettings());
             Connection connection = dataSource.getConnection()) {
            DatabaseInfo databaseInfo = metadataExtractorRegistry.getExtractor(connectionInfo.getType()).extract(connection, connectionInfo);
            log.info("目标库元数据提取完成，数据库类型：{}，数据库：{}，表数量：{}，耗时：{} ms",
                    connectionInfo.getType().name(),
                    connectionInfo.getDatabase(),
                    databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size(),
                    System.currentTimeMillis() - startTime);
            return databaseInfo;
        } catch (SQLException e) {
            throw new BusinessException(ResultCode.METADATA_EXTRACT_FAILED, "元数据抽取失败: " + e.getMessage(), e);
        }
    }

    private TargetConnectionPoolSettings buildPoolSettings() {
        return TargetConnectionPoolSettings.builder()
                .minimumIdle(targetConnectionPoolProperties.getMinimumIdle())
                .maximumPoolSize(targetConnectionPoolProperties.getMaximumPoolSize())
                .connectionTimeoutMs(targetConnectionPoolProperties.getConnectionTimeoutMs())
                .validationTimeoutMs(targetConnectionPoolProperties.getValidationTimeoutMs())
                .idleTimeoutMs(targetConnectionPoolProperties.getIdleTimeoutMs())
                .maxLifetimeMs(targetConnectionPoolProperties.getMaxLifetimeMs())
                .build();
    }
}


