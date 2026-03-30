package com.dbmetadoc.app.service;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.app.repository.DatasourceProfileRepository;
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
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 数据源模板服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DatasourceService {

    private final DatasourceProfileRepository datasourceProfileRepository;
    private final TargetDatabaseService targetDatabaseService;
    private final MetadataExtractorRegistry metadataExtractorRegistry;
    private final ConnectionInfoResolver connectionInfoResolver;
    private final PasswordCipherService passwordCipherService;
    private final TaskExecutor taskExecutor;

    public List<DatasourceDetailResponse> list() {
        return datasourceProfileRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public DatasourceDetailResponse detail(Long id) {
        return findProfile(id).map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ResultCode.DATASOURCE_NOT_FOUND, "数据源不存在: " + id));
    }

    public void test(DatasourceTestRequest request) {
        testAsync(request).join();
    }

    public CompletableFuture<Void> testAsync(DatasourceTestRequest request) {
        Long referenceId = resolveProfileReferenceId(request);
        return CompletableFuture.supplyAsync(() -> resolveConnectionInfo(request), taskExecutor)
                .thenCompose(resolved -> targetDatabaseService.testConnectionAsync(resolved.toDatabaseConnectionInfo()))
                .thenRun(() -> touchIfNecessary(referenceId));
    }

    public DatasourceDetailResponse save(DatasourceSaveRequest request) {
        return saveAsync(request).join();
    }

    public CompletableFuture<DatasourceDetailResponse> saveAsync(DatasourceSaveRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Long referenceId = resolveProfileReferenceId(request);
            DatasourceProfile existingProfile = referenceId == null ? null : findProfile(referenceId)
                    .orElseThrow(() -> new BusinessException(ResultCode.DATASOURCE_NOT_FOUND, "待更新的数据源不存在: " + referenceId));
            return SaveContext.builder()
                    .existingProfile(existingProfile)
                    .resolvedConnectionInfo(resolveConnectionInfo(request))
                    .build();
        }, taskExecutor).thenCompose(context -> targetDatabaseService
                .testConnectionAsync(context.getResolvedConnectionInfo().toDatabaseConnectionInfo())
                .thenApply(ignored -> persistDatasource(request,
                        context.getResolvedConnectionInfo().toDatabaseConnectionInfo(),
                        context.getExistingProfile())));
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
        DatasourceProfile profile = loadProfileIfPresent(resolveProfileReferenceId(request)).orElse(null);
        return connectionInfoResolver.resolve(mergeRequest(request, profile));
    }

    public void touch(Long datasourceId) {
        touchIfNecessary(datasourceId);
    }

    private DatasourceDetailResponse persistDatasource(DatasourceSaveRequest request,
                                                       DatabaseConnectionInfo connectionInfo,
                                                       DatasourceProfile existingProfile) {
        DriverDescriptor descriptor = metadataExtractorRegistry.getDriverDescriptor(connectionInfo.getType());
        log.info("保存数据源模板，名称：{}，数据库类型：{}，主机：{}，数据库：{}，Schema：{}，保存密码：{}",
                request.getName(),
                connectionInfo.getType().name(),
                connectionInfo.getHost(),
                connectionInfo.getDatabase(),
                connectionInfo.getSchema(),
                Boolean.TRUE.equals(request.getRememberPassword()));

        DatasourceProfile profile = datasourceProfileRepository.save(DatasourceProfile.builder()
                .id(request.getId())
                .name(request.getName())
                .dbType(connectionInfo.getType().name())
                .host(connectionInfo.getHost())
                .port(connectionInfo.getEffectivePort())
                .databaseName(connectionInfo.getDatabase())
                .schemaName(connectionInfo.getSchema())
                .username(connectionInfo.getUsername())
                .passwordCipher(resolvePasswordCipher(request, existingProfile))
                .driverClass(StrUtil.blankToDefault(request.getDriverClass(), descriptor.getDriverClass()))
                .remark(request.getRemark())
                .enabled(ObjectUtil.defaultIfNull(request.getEnabled(), Boolean.TRUE))
                .deleted(Boolean.FALSE)
                .build());
        return toResponse(profile);
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
                .passwordSaved(StrUtil.isNotBlank(profile.getPasswordCipher()))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private ConnectionRequest mergeRequest(ConnectionRequest request, DatasourceProfile profile) {
        ConnectionRequest merged = new ConnectionRequest();
        merged.setDatasourceId(resolveProfileReferenceId(request));
        merged.setDbType(firstText(request.getDbType(), profileValue(profile, DatasourceProfile::getDbType)));
        merged.setJdbcUrl(request.getJdbcUrl());
        merged.setHost(firstText(request.getHost(), profileValue(profile, DatasourceProfile::getHost)));
        merged.setPort(ObjectUtil.defaultIfNull(request.getPort(), profileValue(profile, DatasourceProfile::getPort)));
        merged.setDatabase(firstText(request.getDatabase(), profileValue(profile, DatasourceProfile::getDatabaseName)));
        merged.setSchema(firstText(request.getSchema(), profileValue(profile, DatasourceProfile::getSchemaName)));
        merged.setUsername(firstText(request.getUsername(), profileValue(profile, DatasourceProfile::getUsername)));
        merged.setPassword(resolvePassword(request, profile));
        merged.setUseCache(request.getUseCache());
        merged.setForceRefresh(request.getForceRefresh());
        merged.setUseStoredPassword(request.getUseStoredPassword());
        return merged;
    }

    private String resolvePassword(ConnectionRequest request, DatasourceProfile profile) {
        if (StrUtil.isNotBlank(request.getPassword())) {
            return request.getPassword();
        }
        if (Boolean.TRUE.equals(request.getUseStoredPassword()) && profile != null && StrUtil.isNotBlank(profile.getPasswordCipher())) {
            return passwordCipherService.decrypt(profile.getPasswordCipher());
        }
        if (Boolean.TRUE.equals(request.getUseStoredPassword()) && profile != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前模板未保存密码，请输入密码后重试");
        }
        return null;
    }

    private String resolvePasswordCipher(DatasourceSaveRequest request, DatasourceProfile existingProfile) {
        if (BooleanUtil.isFalse(request.getRememberPassword())) {
            return null;
        }
        if (BooleanUtil.isTrue(request.getRememberPassword()) && StrUtil.isNotBlank(request.getPassword())) {
            return passwordCipherService.encrypt(request.getPassword());
        }
        return existingProfile == null ? null : existingProfile.getPasswordCipher();
    }

    private Optional<DatasourceProfile> loadProfileIfPresent(Long datasourceId) {
        if (datasourceId == null) {
            return Optional.empty();
        }
        return Optional.of(findProfile(datasourceId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATASOURCE_NOT_FOUND, "待更新的数据源不存在: " + datasourceId)));
    }

    private Optional<DatasourceProfile> findProfile(Long datasourceId) {
        return datasourceProfileRepository.findById(datasourceId);
    }

    private void touchIfNecessary(Long datasourceId) {
        if (datasourceId != null) {
            datasourceProfileRepository.touch(datasourceId);
        }
    }

    private Long resolveProfileReferenceId(ConnectionRequest request) {
        if (request.getDatasourceId() != null) {
            return request.getDatasourceId();
        }
        if (request instanceof DatasourceSaveRequest datasourceSaveRequest) {
            return datasourceSaveRequest.getId();
        }
        return null;
    }

    private String firstText(String first, String second) {
        return ObjectUtil.defaultIfNull(StrUtil.trimToNull(first), StrUtil.trimToNull(second));
    }

    private <T> T profileValue(DatasourceProfile profile, java.util.function.Function<DatasourceProfile, T> getter) {
        return ObjectUtil.isNull(profile) ? null : getter.apply(profile);
    }

    @Value
    @Builder
    private static class SaveContext {
        DatasourceProfile existingProfile;
        ResolvedConnectionInfo resolvedConnectionInfo;
    }
}


