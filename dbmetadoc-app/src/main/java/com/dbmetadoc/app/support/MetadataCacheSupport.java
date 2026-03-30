package com.dbmetadoc.app.support;

import com.dbmetadoc.db.core.DatabaseConnectionInfo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 元数据缓存辅助工具，统一处理缓存键和摘要计算。
 *
 * @author mumu
 * @date 2026-03-30
 */
public final class MetadataCacheSupport {

    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String CACHE_KEY_SEPARATOR = "|";
    private static final String EMPTY_VALUE = "";

    private MetadataCacheSupport() {
    }

    public static String buildCacheKey(DatabaseConnectionInfo connectionInfo) {
        Objects.requireNonNull(connectionInfo, "connectionInfo must not be null");
        String raw = String.join(CACHE_KEY_SEPARATOR,
                connectionInfo.getType().name(),
                safe(connectionInfo.getHost()),
                String.valueOf(connectionInfo.getEffectivePort()),
                safe(connectionInfo.getDatabase()),
                safe(connectionInfo.getSchema()),
                safe(connectionInfo.getUsername()));
        return sha256(raw);
    }

    public static String buildMetadataHash(String metadataJson) {
        return sha256(metadataJson);
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(safe(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String safe(String value) {
        return value == null ? EMPTY_VALUE : value;
    }
}


