package com.dbmetadoc.app.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.dto.ConnectionRequest;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.JdbcUrlParts;
import com.dbmetadoc.db.core.ResolvedConnectionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 连接参数归一化服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Slf4j
@Component
public class ConnectionInfoResolver {

    private static final Pattern ORACLE_SERVICE_PATTERN =
            Pattern.compile("^//(?<host>[^:/?#,]+)(?::(?<port>\\d+))?/(?<database>[^?]+)(?:\\?(?<query>.*))?$");

    private static final Pattern ORACLE_SID_PATTERN =
            Pattern.compile("^(?<host>[^:/?#,]+)(?::(?<port>\\d+))?:(?<database>[^?]+)(?:\\?(?<query>.*))?$");

    public ResolvedConnectionInfo resolve(ConnectionRequest request) {
        DatabaseType databaseType = DatabaseType.fromCode(request.getDbType());
        JdbcUrlParts jdbcUrlParts = parseJdbcUrl(databaseType, request.getJdbcUrl());

        String host = pickText(request.getHost(), jdbcUrlParts.getHost());
        Integer port = ObjectUtil.defaultIfNull(request.getPort(), jdbcUrlParts.getPort());
        String database = pickText(request.getDatabase(), jdbcUrlParts.getDatabase());
        String schema = pickText(request.getSchema(), jdbcUrlParts.getSchema());

        if (databaseType == DatabaseType.POSTGRESQL) {
            schema = pickText(schema,
                    jdbcUrlParts.getParameters().get("currentSchema"),
                    jdbcUrlParts.getParameters().get("currentschema"),
                    jdbcUrlParts.getParameters().get("current_schema"),
                    "public");
        } else if (databaseType == DatabaseType.KINGBASE) {
            schema = pickText(schema,
                    jdbcUrlParts.getParameters().get("currentSchema"),
                    jdbcUrlParts.getParameters().get("currentschema"),
                    jdbcUrlParts.getParameters().get("current_schema"));
        } else if (databaseType == DatabaseType.ORACLE || databaseType == DatabaseType.DAMENG) {
            schema = pickText(schema, StrUtil.isBlank(request.getUsername()) ? null : request.getUsername().toUpperCase());
        }

        validateResolvedFields(databaseType, host, database);

        DatabaseConnectionInfo connectionInfo = DatabaseConnectionInfo.builder()
                .type(databaseType)
                .jdbcUrl(StrUtil.trimToNull(request.getJdbcUrl()))
                .host(host)
                .port(port)
                .database(database)
                .schema(StrUtil.trimToNull(schema))
                .username(request.getUsername())
                .password(request.getPassword())
                .catalogName(resolveCatalogName(databaseType, database))
                .schemaName(resolveSchemaName(databaseType, schema))
                .serviceNameOrSid(pickText(jdbcUrlParts.getServiceNameOrSid(), database))
                .sidMode(jdbcUrlParts.isSidMode())
                .jdbcParameters(new LinkedHashMap<>(jdbcUrlParts.getParameters()))
                .build();
        connectionInfo.setResolvedJdbcUrl(databaseType.buildJdbcUrl(connectionInfo));
        validateResolvedFields(connectionInfo);

        ResolvedConnectionInfo resolved = ResolvedConnectionInfo.builder()
                .type(databaseType)
                .jdbcUrl(StrUtil.trimToNull(request.getJdbcUrl()))
                .host(connectionInfo.getHost())
                .port(connectionInfo.getPort())
                .database(connectionInfo.getDatabase())
                .schema(connectionInfo.getSchema())
                .username(connectionInfo.getUsername())
                .password(connectionInfo.getPassword())
                .catalogName(connectionInfo.getCatalogName())
                .schemaName(connectionInfo.getSchemaName())
                .serviceNameOrSid(connectionInfo.getServiceNameOrSid())
                .sidMode(connectionInfo.isSidMode())
                .jdbcParameters(new LinkedHashMap<>(connectionInfo.getJdbcParameters()))
                .jdbcUrlParts(jdbcUrlParts)
                .resolvedJdbcUrl(connectionInfo.getResolvedJdbcUrl())
                .build();

        log.info("连接参数归一化完成，数据库类型：{}，主机：{}，端口：{}，数据库：{}，Schema：{}，是否提供JDBC URL：{}",
                databaseType.name(), host, connectionInfo.getEffectivePort(), database, schema,
                StrUtil.isNotBlank(request.getJdbcUrl()));
        log.debug("归一化后的 JDBC URL：{}", connectionInfo.getResolvedJdbcUrl());
        return resolved;
    }

    private JdbcUrlParts parseJdbcUrl(DatabaseType databaseType, String jdbcUrl) {
        if (StrUtil.isBlank(jdbcUrl)) {
            return JdbcUrlParts.builder().parameters(new LinkedHashMap<>()).build();
        }
        try {
            return switch (databaseType) {
                case MYSQL -> parseUriStyle("jdbc:mysql:", jdbcUrl, null);
                case POSTGRESQL -> parseUriStyle("jdbc:postgresql:", jdbcUrl, "currentSchema");
                case KINGBASE -> parseUriStyle("jdbc:kingbase8:", jdbcUrl, "currentSchema");
                case DAMENG -> parseUriStyle("jdbc:dm:", jdbcUrl, "schema");
                case ORACLE -> parseOracleUrl(jdbcUrl);
            };
        } catch (RuntimeException ex) {
            log.warn("JDBC URL 解析失败，将继续使用结构化字段，类型：{}，原因：{}", databaseType.name(), ex.getMessage());
            return JdbcUrlParts.builder().parameters(new LinkedHashMap<>()).build();
        }
    }

    private JdbcUrlParts parseUriStyle(String prefix, String jdbcUrl, String schemaParameter) {
        String body = stripPrefix(prefix, jdbcUrl);
        URI uri = URI.create(body);
        String host = firstHost(uri.getHost());
        Integer port = uri.getPort() > 0 ? uri.getPort() : null;
        String path = StrUtil.removePrefix(uri.getPath(), "/");
        Map<String, String> parameters = parseQuery(uri.getRawQuery());
        String schema = schemaParameter == null ? null : parameters.get(schemaParameter);
        return JdbcUrlParts.builder()
                .host(host)
                .port(port)
                .database(StrUtil.trimToNull(path))
                .schema(StrUtil.trimToNull(schema))
                .serviceNameOrSid(StrUtil.trimToNull(path))
                .parameters(parameters)
                .build();
    }

    private JdbcUrlParts parseOracleUrl(String jdbcUrl) {
        String body = stripPrefix("jdbc:oracle:thin:@", jdbcUrl);
        Matcher serviceMatcher = ORACLE_SERVICE_PATTERN.matcher(body);
        if (serviceMatcher.matches()) {
            return JdbcUrlParts.builder()
                    .host(serviceMatcher.group("host"))
                    .port(parseInteger(serviceMatcher.group("port")))
                    .database(serviceMatcher.group("database"))
                    .serviceNameOrSid(serviceMatcher.group("database"))
                    .sidMode(false)
                    .parameters(parseQuery(serviceMatcher.group("query")))
                    .build();
        }
        Matcher sidMatcher = ORACLE_SID_PATTERN.matcher(body);
        if (sidMatcher.matches()) {
            return JdbcUrlParts.builder()
                    .host(sidMatcher.group("host"))
                    .port(parseInteger(sidMatcher.group("port")))
                    .database(sidMatcher.group("database"))
                    .serviceNameOrSid(sidMatcher.group("database"))
                    .sidMode(true)
                    .parameters(parseQuery(sidMatcher.group("query")))
                    .build();
        }
        throw new IllegalArgumentException("暂不支持的 Oracle JDBC URL 形式");
    }

    private String stripPrefix(String prefix, String jdbcUrl) {
        if (!StrUtil.startWithIgnoreCase(jdbcUrl, prefix)) {
            throw new IllegalArgumentException("JDBC URL 前缀不匹配");
        }
        return jdbcUrl.substring(prefix.length());
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (StrUtil.isBlank(query)) {
            return parameters;
        }
        for (String pair : StrUtil.split(query, '&')) {
            if (StrUtil.isBlank(pair)) {
                continue;
            }
            String[] items = StrUtil.splitToArray(pair, '=');
            String key = decode(items[0]);
            String value = items.length > 1 ? decode(items[1]) : "";
            parameters.put(key, value);
        }
        return parameters;
    }

    private String decode(String value) {
        return URLDecoder.decode(StrUtil.blankToDefault(value, ""), StandardCharsets.UTF_8);
    }

    private String firstHost(String host) {
        if (StrUtil.isBlank(host)) {
            return null;
        }
        return StrUtil.splitTrim(host, ',').get(0);
    }

    private Integer parseInteger(String value) {
        return StrUtil.isBlank(value) ? null : Integer.parseInt(value);
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

    private void validateResolvedFields(DatabaseType databaseType, String host, String database) {
        if (StrUtil.isBlank(host)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "主机地址不能为空，未提供结构化字段且无法从 JDBC URL 中解析");
        }
        if (StrUtil.isBlank(database)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    StrUtil.format("{} 的数据库/服务名不能为空，未提供结构化字段且无法从 JDBC URL 中解析", databaseType.getLabel()));
        }
    }

    private void validateResolvedFields(DatabaseConnectionInfo connectionInfo) {
        if (StrUtil.isBlank(connectionInfo.getUsername())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名不能为空");
        }
        if (StrUtil.isBlank(connectionInfo.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空，若使用模板密码请勾选“使用已保存密码”");
        }
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


