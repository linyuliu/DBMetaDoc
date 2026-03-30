package com.dbmetadoc.app.service;

import com.alibaba.fastjson2.JSON;
import com.dbmetadoc.app.support.MetadataCacheSupport;
import com.dbmetadoc.common.entity.MetadataCacheRecord;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.app.properties.MetadataCacheProperties;
import com.dbmetadoc.app.repository.MetadataCacheRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 元数据缓存服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataCacheService {

    private final MetadataCacheRepository metadataCacheRepository;
    private final MetadataCacheProperties metadataCacheProperties;

    public Optional<DatabaseInfo> find(DatabaseConnectionInfo connectionInfo) {
        String cacheKey = MetadataCacheSupport.buildCacheKey(connectionInfo);
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
                .cacheKey(MetadataCacheSupport.buildCacheKey(connectionInfo))
                .cacheHash(MetadataCacheSupport.buildMetadataHash(metadataJson))
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
}


