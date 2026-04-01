package com.dbmetadoc.app.service.jdbc;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.dto.ConnectionRequest;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.JdbcUrlParts;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 合并结构化字段与 JDBC URL，生成统一的连接字段视图。
 *
 * @author mumu
 * @date 2026-04-01
 */
public final class JdbcConnectionFieldResolver {

    private static final List<String> PG_SCHEMA_KEYS = List.of("currentSchema", "current_schema");
    private static final List<String> DAMENG_SCHEMA_KEYS = List.of("schema");

    private final JdbcMetadataParameterAugmenter metadataParameterAugmenter = new JdbcMetadataParameterAugmenter();

    /**
     * 结构化字段优先，其次取 JDBC URL，最后按数据库方言补默认值。
     */
    public JdbcResolvedFields resolve(ConnectionRequest request,
                                      ParsedJdbcUrl parsedJdbcUrl,
                                      DatabaseType databaseType) {
        JdbcUrlParts jdbcUrlParts = parsedJdbcUrl.parts();
        String host = pickText(request.getHost(), jdbcUrlParts.getHost());
        Integer port = ObjectUtil.defaultIfNull(request.getPort(), jdbcUrlParts.getPort());
        String database = pickText(request.getDatabase(), jdbcUrlParts.getDatabase());
        String schema = resolveSchema(request, jdbcUrlParts, databaseType);

        return new JdbcResolvedFields(
                host,
                port,
                database,
                schema,
                resolveCatalogName(databaseType, database),
                resolveSchemaName(databaseType, schema),
                pickText(jdbcUrlParts.getServiceNameOrSid(), database),
                jdbcUrlParts.isSidMode(),
                metadataParameterAugmenter.augment(databaseType, jdbcUrlParts.getParameters()),
                jdbcUrlParts
        );
    }

    private String resolveSchema(ConnectionRequest request, JdbcUrlParts jdbcUrlParts, DatabaseType databaseType) {
        String schema = pickText(request.getSchema(), jdbcUrlParts.getSchema());
        return switch (databaseType) {
            case POSTGRESQL -> pickText(schema, pickParameter(jdbcUrlParts.getParameters(), PG_SCHEMA_KEYS), "public");
            case KINGBASE -> pickText(schema, pickParameter(jdbcUrlParts.getParameters(), PG_SCHEMA_KEYS));
            case DAMENG -> pickText(schema, pickParameter(jdbcUrlParts.getParameters(), DAMENG_SCHEMA_KEYS),
                    uppercaseUsername(request.getUsername()));
            case ORACLE -> pickText(schema, uppercaseUsername(request.getUsername()));
            case MYSQL -> schema;
        };
    }

    private String resolveCatalogName(DatabaseType databaseType, String database) {
        return switch (databaseType) {
            case MYSQL, POSTGRESQL, KINGBASE -> database;
            case ORACLE, DAMENG -> null;
        };
    }

    private String resolveSchemaName(DatabaseType databaseType, String schema) {
        return switch (databaseType) {
            case MYSQL -> null;
            default -> StrUtil.trimToNull(schema);
        };
    }

    private String pickParameter(Map<String, String> parameters, List<String> keys) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            if (StrUtil.isBlank(key)) {
                continue;
            }
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key) && StrUtil.isNotBlank(entry.getValue())) {
                    return entry.getValue().trim();
                }
            }
        }
        return null;
    }

    private String uppercaseUsername(String username) {
        return StrUtil.isBlank(username) ? null : username.trim().toUpperCase(Locale.ROOT);
    }

    private String pickText(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
