package com.dbmetadoc.app.service.jdbc;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.dto.ConnectionRequest;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.JdbcUrlParts;
import com.dbmetadoc.db.core.ResolvedConnectionInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化字段与 JDBC URL 合并后的中间结果。
 * <p>
 * 该对象只关注“解析和字段归一化”，真正的 JDBC URL 组装与校验仍由上层完成。
 * </p>
 *
 * @author mumu
 * @date 2026-04-01
 */
public record JdbcResolvedFields(String host,
                                 Integer port,
                                 String database,
                                 String schema,
                                 String catalogName,
                                 String schemaName,
                                 String serviceNameOrSid,
                                 boolean sidMode,
                                 Map<String, String> jdbcParameters,
                                 JdbcUrlParts jdbcUrlParts) {

    public JdbcResolvedFields {
        jdbcParameters = jdbcParameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(jdbcParameters);
        jdbcUrlParts = jdbcUrlParts == null
                ? JdbcUrlParts.builder().parameters(new LinkedHashMap<>()).build()
                : jdbcUrlParts;
    }

    /**
     * 转换为底层连接对象，用于后续 JDBC URL 组装和真实连接。
     */
    public DatabaseConnectionInfo toDatabaseConnectionInfo(ConnectionRequest request, DatabaseType databaseType) {
        return DatabaseConnectionInfo.builder()
                .type(databaseType)
                .jdbcUrl(StrUtil.trimToNull(request.getJdbcUrl()))
                .host(host)
                .port(port)
                .database(database)
                .schema(schema)
                .username(request.getUsername())
                .password(request.getPassword())
                .catalogName(catalogName)
                .schemaName(schemaName)
                .serviceNameOrSid(serviceNameOrSid)
                .sidMode(sidMode)
                .jdbcParameters(new LinkedHashMap<>(jdbcParameters))
                .build();
    }

    /**
     * 转换为对上层接口公开的已解析连接信息。
     */
    public ResolvedConnectionInfo toResolvedConnectionInfo(ConnectionRequest request,
                                                           DatabaseType databaseType,
                                                           String resolvedJdbcUrl) {
        return ResolvedConnectionInfo.builder()
                .type(databaseType)
                .jdbcUrl(StrUtil.trimToNull(request.getJdbcUrl()))
                .host(host)
                .port(port)
                .database(database)
                .schema(schema)
                .username(request.getUsername())
                .password(request.getPassword())
                .catalogName(catalogName)
                .schemaName(schemaName)
                .serviceNameOrSid(serviceNameOrSid)
                .sidMode(sidMode)
                .jdbcParameters(new LinkedHashMap<>(jdbcParameters))
                .jdbcUrlParts(jdbcUrlParts)
                .resolvedJdbcUrl(resolvedJdbcUrl)
                .build();
    }
}
