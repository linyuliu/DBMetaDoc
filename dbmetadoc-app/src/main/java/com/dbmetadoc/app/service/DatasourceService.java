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
import com.dbmetadoc.app.repository.DatasourceProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据源模板服务。
 */
@Service
@RequiredArgsConstructor
public class DatasourceService {

    private final DatasourceProfileRepository datasourceProfileRepository;
    private final TargetDatabaseService targetDatabaseService;
    private final MetadataExtractorRegistry metadataExtractorRegistry;

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
        targetDatabaseService.testConnection(toConnectionInfo(request));
    }

    public DatasourceDetailResponse save(DatasourceSaveRequest request) {
        if (request.getId() != null && datasourceProfileRepository.findById(request.getId()).isEmpty()) {
            throw new BusinessException(ResultCode.DATASOURCE_NOT_FOUND, "待更新的数据源不存在: " + request.getId());
        }
        DatabaseConnectionInfo connectionInfo = toConnectionInfo(request);
        targetDatabaseService.testConnection(connectionInfo);
        DriverDescriptor descriptor = metadataExtractorRegistry.getDriverDescriptor(connectionInfo.getType());

        DatasourceProfile profile = datasourceProfileRepository.save(DatasourceProfile.builder()
                .id(request.getId())
                .name(request.getName())
                .dbType(connectionInfo.getType().name())
                .host(request.getHost())
                .port(connectionInfo.getEffectivePort())
                .databaseName(request.getDatabase())
                .schemaName(request.getSchema())
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
        DatabaseType databaseType = DatabaseType.fromCode(request.getDbType());
        return DatabaseConnectionInfo.builder()
                .type(databaseType)
                .host(request.getHost())
                .port(request.getPort())
                .database(request.getDatabase())
                .schema(request.getSchema())
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
    }

    private DatasourceDetailResponse toResponse(DatasourceProfile profile) {
        DatabaseType databaseType = DatabaseType.fromCode(profile.getDbType());
        DriverDescriptor descriptor = metadataExtractorRegistry.getDriverDescriptor(databaseType);
        return DatasourceDetailResponse.builder()
                .id(profile.getId())
                .name(profile.getName())
                .dbType(profile.getDbType())
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
