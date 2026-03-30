package com.dbmetadoc.app.repository;

import com.dbmetadoc.common.entity.MetadataCacheRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * 元数据缓存仓储。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Repository
@RequiredArgsConstructor
public class MetadataCacheRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MetadataCacheRecord> rowMapper = (rs, rowNum) -> MetadataCacheRecord.builder()
            .id(rs.getLong("id"))
            .cacheKey(rs.getString("cache_key"))
            .cacheHash(rs.getString("cache_hash"))
            .dbType(rs.getString("db_type"))
            .databaseName(rs.getString("database_name"))
            .schemaName(rs.getString("schema_name"))
            .metadataJson(rs.getString("metadata_json"))
            .syncedAt(toLocalDateTime(rs.getTimestamp("synced_at")))
            .expiresAt(toLocalDateTime(rs.getTimestamp("expires_at")))
            .build();

    public Optional<MetadataCacheRecord> findValidByCacheKey(String cacheKey, LocalDateTime now) {
        String sql = "SELECT id, cache_key, cache_hash, db_type, database_name, schema_name, metadata_json, synced_at, expires_at "
                + "FROM md_metadata_cache WHERE cache_key = ? AND expires_at > ?";
        List<MetadataCacheRecord> result = jdbcTemplate.query(sql, rowMapper, cacheKey, Timestamp.valueOf(now));
        return result.stream().findFirst();
    }

    public void save(MetadataCacheRecord record) {
        String sql = "INSERT INTO md_metadata_cache "
                + "(cache_key, cache_hash, db_type, database_name, schema_name, metadata_json, synced_at, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "cache_hash = VALUES(cache_hash), db_type = VALUES(db_type), database_name = VALUES(database_name), "
                + "schema_name = VALUES(schema_name), metadata_json = VALUES(metadata_json), "
                + "synced_at = VALUES(synced_at), expires_at = VALUES(expires_at)";
        jdbcTemplate.update(sql,
                record.getCacheKey(),
                record.getCacheHash(),
                record.getDbType(),
                record.getDatabaseName(),
                record.getSchemaName(),
                record.getMetadataJson(),
                Timestamp.valueOf(record.getSyncedAt()),
                Timestamp.valueOf(record.getExpiresAt()));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
