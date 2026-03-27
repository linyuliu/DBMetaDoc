package com.dbmetadoc.app.service;

import com.alibaba.fastjson2.JSON;
import com.dbmetadoc.common.entity.MetadataCacheRecord;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.app.properties.MetadataCacheProperties;
import com.dbmetadoc.app.repository.MetadataCacheRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataCacheService {

    private final MetadataCacheRepository metadataCacheRepository;
    private final MetadataCacheProperties metadataCacheProperties;

    public Optional<DatabaseInfo> find(DatabaseConnectionInfo connectionInfo) {
        String cacheKey = buildCacheKey(connectionInfo);
        Optional<DatabaseInfo> result = metadataCacheRepository.findValidByCacheKey(cacheKey, LocalDateTime.now())
                .map(MetadataCacheRecord::getMetadataJson)
                .map(json -> JSON.parseObject(json, DatabaseInfo.class));
        log.debug("查询元数据缓存，数据库类型：{}，数据库：{}，Schema：{}，命中：{}",
                connectionInfo.getType().name(), connectionInfo.getDatabase(), connectionInfo.getSchema(), result.isPresent());
        return result;
    }

    public void save(DatabaseConnectionInfo connectionInfo, DatabaseInfo databaseInfo) {
        String metadataJson = JSON.toJSONString(databaseInfo);
        LocalDateTime syncedAt = LocalDateTime.now();
        metadataCacheRepository.save(MetadataCacheRecord.builder()
                .cacheKey(buildCacheKey(connectionInfo))
                .cacheHash(sha256(metadataJson))
                .dbType(connectionInfo.getType().name())
                .databaseName(connectionInfo.getDatabase())
                .schemaName(connectionInfo.getSchema())
                .metadataJson(metadataJson)
                .syncedAt(syncedAt)
                .expiresAt(syncedAt.plusHours(metadataCacheProperties.getTtlHours()))
                .build());
        log.info("写入元数据缓存，数据库类型：{}，数据库：{}，Schema：{}，过期时间：{}",
                connectionInfo.getType().name(), connectionInfo.getDatabase(), connectionInfo.getSchema(),
                syncedAt.plusHours(metadataCacheProperties.getTtlHours()));
    }

    private String buildCacheKey(DatabaseConnectionInfo connectionInfo) {
        String raw = String.join("|",
                connectionInfo.getType().name(),
                safe(connectionInfo.getHost()),
                String.valueOf(connectionInfo.getEffectivePort()),
                safe(connectionInfo.getDatabase()),
                safe(connectionInfo.getSchema()),
                safe(connectionInfo.getUsername()));
        return sha256(raw);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
