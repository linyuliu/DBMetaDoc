package com.dbmetadoc.app.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.dto.ConnectionRequest;
import com.dbmetadoc.common.dto.DatasourceSaveRequest;
import com.dbmetadoc.common.dto.DatasourceTestRequest;
import com.dbmetadoc.common.entity.DatasourceProfile;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.common.vo.DatasourceDetailResponse;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.DriverDescriptor;
import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import com.dbmetadoc.db.core.ResolvedConnectionInfo;
import com.dbmetadoc.app.repository.DatasourceProfileRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据源模板服务。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DatasourceService {

    private final DatasourceProfileRepository datasourceProfileRepository;
    private final TargetDatabaseService targetDatabaseService;
    private final MetadataExtractorRegistry metadataExtractorRegistry;
    private final ConnectionInfoResolver connectionInfoResolver;

    public List<DatasourceDetailResponse> list() {
        return datasourceProfileRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public DatasourceDetailResponse detail(Long id) {
        return datasourceProfileRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ResultCode.DATASOURCE_NOT_FOUND, "数据源不存在: " + id));
    }

    public void test(DatasourceTestRequest request) {
        ResolvedConnectionInfo resolvedConnectionInfo = resolveConnectionInfo(request);
        targetDatabaseService.testConnection(resolvedConnectionInfo.toDatabaseConnectionInfo());
    }

    public DatasourceDetailResponse save(DatasourceSaveRequest request) {
        if (request.getId() != null && datasourceProfileRepository.findById(request.getId()).isEmpty()) {
            throw new BusinessException(ResultCode.DATASOURCE_NOT_FOUND, "待更新的数据源不存在: " + request.getId());
        }
        ResolvedConnectionInfo resolvedConnectionInfo = resolveConnectionInfo(request);
        DatabaseConnectionInfo connectionInfo = resolvedConnectionInfo.toDatabaseConnectionInfo();
        targetDatabaseService.testConnection(connectionInfo);
        DriverDescriptor descriptor = metadataExtractorRegistry.getDriverDescriptor(connectionInfo.getType());
        log.info("保存数据源模板，名称：{}，数据库类型：{}，主机：{}，数据库：{}，Schema：{}",
                request.getName(), connectionInfo.getType().name(), connectionInfo.getHost(),
                connectionInfo.getDatabase(), connectionInfo.getSchema());

        DatasourceProfile profile = datasourceProfileRepository.save(DatasourceProfile.builder()
                .id(request.getId())
                .name(request.getName())
                .dbType(connectionInfo.getType().name())
                .host(connectionInfo.getHost())
                .port(connectionInfo.getEffectivePort())
                .databaseName(connectionInfo.getDatabase())
                .schemaName(connectionInfo.getSchema())
                .username(request.getUsername())
                .driverClass(StrUtil.blankToDefault(request.getDriverClass(), descriptor.getDriverClass()))
                .remark(request.getRemark())
                .enabled(ObjectUtil.defaultIfNull(request.getEnabled(), Boolean.TRUE))
                .deleted(Boolean.FALSE)
                .build());
        return toResponse(profile);
    }

    public void remove(Long id) {
        if (datasourceProfileRepository.softDelete(id) == 0) {
            throw new BusinessException(ResultCode.DATASOURCE_NOT_FOUND, "数据源不存在: " + id);
        }
    }

    public DatabaseConnectionInfo toConnectionInfo(ConnectionRequest request) {
        return resolveConnectionInfo(request).toDatabaseConnectionInfo();
    }

    public ResolvedConnectionInfo resolveConnectionInfo(ConnectionRequest request) {
        return connectionInfoResolver.resolve(request);
    }

    private DatasourceDetailResponse toResponse(DatasourceProfile profile) {
        DatabaseType databaseType = DatabaseType.fromCode(profile.getDbType());
        DriverDescriptor descriptor = metadataExtractorRegistry.getDriverDescriptor(databaseType);
        return DatasourceDetailResponse.builder()
                .id(profile.getId())
                .name(profile.getName())
                .dbType(profile.getDbType())
                .jdbcUrl(null)
                .host(profile.getHost())
                .port(profile.getPort())
                .database(profile.getDatabaseName())
                .schema(profile.getSchemaName())
                .username(profile.getUsername())
                .driverClass(StrUtil.blankToDefault(profile.getDriverClass(), descriptor.getDriverClass()))
                .testSql(descriptor.getTestSql())
                .remark(profile.getRemark())
                .enabled(profile.getEnabled())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
