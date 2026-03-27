package com.dbmetadoc.db.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC URL 解析结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdbcUrlParts {

    private String host;

    private Integer port;

    private String database;

    private String schema;

    private String serviceNameOrSid;

    private boolean sidMode;

    @Builder.Default
    private Map<String, String> parameters = new LinkedHashMap<>();
}
