package com.dbmetadoc.app.repository;

import com.dbmetadoc.common.entity.DatasourceProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DatasourceProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DatasourceProfile> rowMapper = (rs, rowNum) -> DatasourceProfile.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .dbType(rs.getString("db_type"))
            .host(rs.getString("host"))
            .port(rs.getInt("port"))
            .databaseName(rs.getString("database_name"))
            .schemaName(rs.getString("schema_name"))
            .username(rs.getString("username"))
            .driverClass(rs.getString("driver_class"))
            .remark(rs.getString("remark"))
            .enabled(rs.getBoolean("enabled"))
            .deleted(rs.getBoolean("deleted"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
            .build();

    public List<DatasourceProfile> findAll() {
        String sql = "SELECT id, name, db_type, host, port, database_name, schema_name, username, driver_class, remark, "
                + "enabled, deleted, created_at, updated_at "
                + "FROM md_datasource_profile WHERE deleted = 0 ORDER BY updated_at DESC, id DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<DatasourceProfile> findById(Long id) {
        String sql = "SELECT id, name, db_type, host, port, database_name, schema_name, username, driver_class, remark, "
                + "enabled, deleted, created_at, updated_at "
                + "FROM md_datasource_profile WHERE id = ? AND deleted = 0";
        List<DatasourceProfile> result = jdbcTemplate.query(sql, rowMapper, id);
        return result.stream().findFirst();
    }

    public DatasourceProfile save(DatasourceProfile profile) {
        if (profile.getId() == null) {
            return insert(profile);
        }
        update(profile);
        return findById(profile.getId()).orElseThrow();
    }

    public int softDelete(Long id) {
        String sql = "UPDATE md_datasource_profile SET deleted = 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND deleted = 0";
        return jdbcTemplate.update(sql, id);
    }

    private DatasourceProfile insert(DatasourceProfile profile) {
        String sql = "INSERT INTO md_datasource_profile "
                + "(name, db_type, host, port, database_name, schema_name, username, driver_class, remark, enabled, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, profile.getName());
            ps.setString(2, profile.getDbType());
            ps.setString(3, profile.getHost());
            ps.setInt(4, profile.getPort());
            ps.setString(5, profile.getDatabaseName());
            ps.setString(6, profile.getSchemaName());
            ps.setString(7, profile.getUsername());
            ps.setString(8, profile.getDriverClass());
            ps.setString(9, profile.getRemark());
            ps.setBoolean(10, Boolean.TRUE.equals(profile.getEnabled()));
            ps.setBoolean(11, Boolean.TRUE.equals(profile.getDeleted()));
            return ps;
        }, keyHolder);
        profile.setId(keyHolder.getKey().longValue());
        return findById(profile.getId()).orElseThrow();
    }

    private void update(DatasourceProfile profile) {
        String sql = "UPDATE md_datasource_profile SET "
                + "name = ?, db_type = ?, host = ?, port = ?, database_name = ?, schema_name = ?, username = ?, "
                + "driver_class = ?, remark = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND deleted = 0";
        jdbcTemplate.update(sql,
                profile.getName(),
                profile.getDbType(),
                profile.getHost(),
                profile.getPort(),
                profile.getDatabaseName(),
                profile.getSchemaName(),
                profile.getUsername(),
                profile.getDriverClass(),
                profile.getRemark(),
                Boolean.TRUE.equals(profile.getEnabled()),
                profile.getId());
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
