package com.dbmetadoc.common.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 元数据缓存记录对象。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataCacheRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String cacheKey;
    private String cacheHash;
    private String dbType;
    private String databaseName;
    private String schemaName;
    private String metadataJson;
    private LocalDateTime syncedAt;
    private LocalDateTime expiresAt;
}
