package com.dbmetadoc.db.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 经过 JDBC URL 解析和字段优先级合并后的连接参数。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedConnectionInfo {

    private DatabaseType type;

    private String jdbcUrl;

    private String host;

    private Integer port;

    private String database;

    private String schema;

    private String username;

    private String password;

    private String catalogName;

    private String schemaName;

    private String serviceNameOrSid;

    private boolean sidMode;

    @Builder.Default
    private Map<String, String> jdbcParameters = new LinkedHashMap<>();

    private JdbcUrlParts jdbcUrlParts;

    private String resolvedJdbcUrl;

    public DatabaseConnectionInfo toDatabaseConnectionInfo() {
        return DatabaseConnectionInfo.builder()
                .type(type)
                .jdbcUrl(jdbcUrl)
                .host(host)
                .port(port)
                .database(database)
                .schema(schema)
                .username(username)
                .password(password)
                .catalogName(catalogName)
                .schemaName(schemaName)
                .serviceNameOrSid(serviceNameOrSid)
                .sidMode(sidMode)
                .jdbcParameters(jdbcParameters)
                .resolvedJdbcUrl(resolvedJdbcUrl)
                .build();
    }
}


