package com.dbmetadoc.app.service;

import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import com.dbmetadoc.db.core.TargetConnectionFactory;
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
@RequiredArgsConstructor
public class TargetDatabaseService {

    private final MetadataExtractorRegistry metadataExtractorRegistry;

    public void testConnection(DatabaseConnectionInfo connectionInfo) {
        try (Connection connection = TargetConnectionFactory.create(connectionInfo);
             PreparedStatement ps = connection.prepareStatement(
                     metadataExtractorRegistry.getExtractor(connectionInfo.getType()).resolveTestSql(connection, connectionInfo));
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new BusinessException(ResultCode.DATASOURCE_TEST_FAILED, "连接测试未返回结果");
            }
        } catch (SQLException e) {
            throw new BusinessException(ResultCode.DATASOURCE_TEST_FAILED, "连接测试失败: " + e.getMessage(), e);
        }
    }

    public DatabaseInfo extract(DatabaseConnectionInfo connectionInfo) {
        try (Connection connection = TargetConnectionFactory.create(connectionInfo)) {
            return metadataExtractorRegistry.getExtractor(connectionInfo.getType()).extract(connection, connectionInfo);
        } catch (SQLException e) {
            throw new BusinessException(ResultCode.METADATA_EXTRACT_FAILED, "元数据抽取失败: " + e.getMessage(), e);
        }
    }
}
