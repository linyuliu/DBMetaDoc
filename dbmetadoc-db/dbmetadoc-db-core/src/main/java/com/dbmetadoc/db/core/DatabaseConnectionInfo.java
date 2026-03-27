package com.dbmetadoc.db.core;

import lombok.Builder.Default;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 目标数据库连接参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConnectionInfo {

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

    @Default
    private Map<String, String> jdbcParameters = new LinkedHashMap<>();

    private String resolvedJdbcUrl;

    /**
     * 获取生效端口，未填写时回退到数据库类型默认端口。
     */
    public int getEffectivePort() {
        return port == null ? type.getDefaultPort() : port;
    }
}
