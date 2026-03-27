package com.dbmetadoc.app.service;

import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import com.dbmetadoc.db.core.TargetConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 目标数据库操作服务。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TargetDatabaseService {

    private final MetadataExtractorRegistry metadataExtractorRegistry;

    public void testConnection(DatabaseConnectionInfo connectionInfo) {
        long startTime = System.currentTimeMillis();
        try (Connection connection = TargetConnectionFactory.create(connectionInfo);
             PreparedStatement ps = connection.prepareStatement(
                     metadataExtractorRegistry.getExtractor(connectionInfo.getType()).resolveTestSql(connection, connectionInfo));
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new BusinessException(ResultCode.DATASOURCE_TEST_FAILED, "连接测试未返回结果");
            }
            log.info("连接测试通过，数据库类型：{}，主机：{}，端口：{}，数据库：{}，耗时：{} ms",
                    connectionInfo.getType().name(), connectionInfo.getHost(), connectionInfo.getEffectivePort(),
                    connectionInfo.getDatabase(), System.currentTimeMillis() - startTime);
        } catch (SQLException e) {
            throw new BusinessException(ResultCode.DATASOURCE_TEST_FAILED, "连接测试失败: " + e.getMessage(), e);
        }
    }

    public DatabaseInfo extract(DatabaseConnectionInfo connectionInfo) {
        long startTime = System.currentTimeMillis();
        try (Connection connection = TargetConnectionFactory.create(connectionInfo)) {
            DatabaseInfo databaseInfo = metadataExtractorRegistry.getExtractor(connectionInfo.getType()).extract(connection, connectionInfo);
            log.info("目标库元数据提取完成，数据库类型：{}，数据库：{}，表数量：{}，耗时：{} ms",
                    connectionInfo.getType().name(), connectionInfo.getDatabase(),
                    databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size(),
                    System.currentTimeMillis() - startTime);
            return databaseInfo;
        } catch (SQLException e) {
            throw new BusinessException(ResultCode.METADATA_EXTRACT_FAILED, "元数据抽取失败: " + e.getMessage(), e);
        }
    }
}
