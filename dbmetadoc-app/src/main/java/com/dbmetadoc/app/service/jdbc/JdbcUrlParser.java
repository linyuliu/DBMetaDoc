package com.dbmetadoc.app.service.jdbc;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.JdbcUrlParts;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDBC URL 解析器。
 * <p>
 * 规则表按照“vendor 规则 + parser kind”组织，方便和前端解析逻辑长期对齐。
 * 常见 vendor 前缀和兼容映射参考了 Druid {@code JdbcUtils} 的识别思路。
 * </p>
 *
 * @author mumu
 * @date 2026-04-01
 */
@Slf4j
public final class JdbcUrlParser {

    private static final String JDBC_PROTOCOL = "jdbc:";
    private static final List<String> JDBC_WRAPPER_PREFIXES = List.of("jdbc:log4jdbc:");

    private static final Pattern ORACLE_SERVICE_PATTERN =
            Pattern.compile("^//(?<host>[^:/?#,;]+)(?::(?<port>\\d+))?/(?<database>[^?;]+)(?<rest>[?;].*)?$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ORACLE_SID_PATTERN =
            Pattern.compile("^(?<host>[^:/?#,;]+)(?::(?<port>\\d+))?:(?<database>[^?;]+)(?<rest>[?;].*)?$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ORACLE_DESCRIPTION_HOST_PATTERN =
            Pattern.compile("\\(HOST\\s*=\\s*(?<value>[^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORACLE_DESCRIPTION_PORT_PATTERN =
            Pattern.compile("\\(PORT\\s*=\\s*(?<value>\\d+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORACLE_DESCRIPTION_SERVICE_PATTERN =
            Pattern.compile("\\(SERVICE_NAME\\s*=\\s*(?<value>[^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORACLE_DESCRIPTION_SID_PATTERN =
            Pattern.compile("\\(SID\\s*=\\s*(?<value>[^)]+)\\)", Pattern.CASE_INSENSITIVE);

    private static final List<JdbcVendorRule> JDBC_VENDOR_RULES = List.of(
            JdbcVendorRule.supported("ALIBABA_ORACLE", List.of("jdbc:alibaba:oracle:"), JdbcParserKind.ORACLE, DatabaseType.ORACLE),
            JdbcVendorRule.supported("ORACLE", List.of("jdbc:oracle:thin:@"), JdbcParserKind.ORACLE, DatabaseType.ORACLE),
            JdbcVendorRule.supported("POSTGRESQL", List.of("jdbc:postgresql:"), JdbcParserKind.URI, DatabaseType.POSTGRESQL,
                    List.of("currentSchema", "current_schema")),
            JdbcVendorRule.supported("KINGBASE", List.of("jdbc:kingbase8:", "jdbc:kingbase:"), JdbcParserKind.URI, DatabaseType.KINGBASE,
                    List.of("currentSchema", "current_schema")),
            JdbcVendorRule.supported("MARIADB", List.of("jdbc:mariadb:"), JdbcParserKind.URI, DatabaseType.MYSQL),
            JdbcVendorRule.supported("MYSQL",
                    List.of("jdbc:mysql+srv:loadbalance:", "jdbc:mysql+srv:replication:", "jdbc:mysql+srv:", "jdbc:mysql:"),
                    JdbcParserKind.URI, DatabaseType.MYSQL),
            JdbcVendorRule.supported("TIDB", List.of("jdbc:tidb:"), JdbcParserKind.URI, DatabaseType.MYSQL),
            JdbcVendorRule.supported("DAMENG", List.of("jdbc:dm:"), JdbcParserKind.URI, DatabaseType.DAMENG, List.of("schema")),
            JdbcVendorRule.supported("EDB", List.of("jdbc:edb:"), JdbcParserKind.URI, DatabaseType.POSTGRESQL,
                    List.of("currentSchema", "current_schema")),
            JdbcVendorRule.unsupported("SQLSERVER", List.of("jdbc:sqlserver:"), JdbcParserKind.SQLSERVER,
                    "已识别为 SQL Server JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器"),
            JdbcVendorRule.unsupported("CLICKHOUSE", List.of("jdbc:clickhouse:"), JdbcParserKind.URI,
                    "已识别为 ClickHouse JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器"),
            JdbcVendorRule.unsupported("DB2", List.of("jdbc:db2:"), JdbcParserKind.URI,
                    "已识别为 DB2 JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器"),
            JdbcVendorRule.unsupported("HIVE", List.of("jdbc:hive2:", "jdbc:hive:"), JdbcParserKind.URI,
                    "已识别为 Hive JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器"),
            JdbcVendorRule.unsupported("PHOENIX", List.of("jdbc:phoenix:thin:", "jdbc:phoenix://"), JdbcParserKind.UNSUPPORTED,
                    "已识别为 Phoenix JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器"),
            JdbcVendorRule.unsupported("KYLIN", List.of("jdbc:kylin:"), JdbcParserKind.URI,
                    "已识别为 Kylin JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器"),
            JdbcVendorRule.unsupported("GBASE", List.of("jdbc:gbase:"), JdbcParserKind.URI,
                    "已识别为 GBase JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器"),
            JdbcVendorRule.unsupported("XUGU", List.of("jdbc:xugu:"), JdbcParserKind.URI,
                    "已识别为虚谷 JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器"),
            JdbcVendorRule.unsupported("OCEANBASE", List.of("jdbc:oceanbase:"), JdbcParserKind.URI,
                    "已识别为 OceanBase JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器")
    );

    /**
     * 解析 JDBC URL。无法识别时返回 {@link ParsedJdbcUrl#unknown()}，由上层继续使用结构化字段兜底。
     */
    public ParsedJdbcUrl parse(String jdbcUrl) {
        if (StrUtil.isBlank(jdbcUrl)) {
            return ParsedJdbcUrl.unknown();
        }
        if (!StrUtil.startWithIgnoreCase(jdbcUrl, JDBC_PROTOCOL)) {
            log.warn("JDBC URL 不是以 jdbc: 开头，将继续使用结构化字段：{}", jdbcUrl);
            return ParsedJdbcUrl.unknown();
        }

        String normalizedUrl = unwrapJdbcUrl(jdbcUrl.trim());
        JdbcRuleMatch ruleMatch = matchRule(normalizedUrl);
        if (ruleMatch == null) {
            log.warn("未识别的 JDBC URL 前缀，将继续使用结构化字段：{}", jdbcUrl);
            return ParsedJdbcUrl.unknown();
        }

        JdbcUrlParts parts;
        try {
            parts = parseByRule(ruleMatch.rule(), normalizedUrl, ruleMatch.matchedPrefix());
        } catch (RuntimeException ex) {
            log.warn("JDBC URL 已识别但解析失败，将继续使用结构化字段，vendor={}，原因={}",
                    ruleMatch.rule().vendorCode(), ex.getMessage());
            parts = emptyJdbcUrlParts();
        }

        return new ParsedJdbcUrl(
                true,
                ruleMatch.rule().vendorCode(),
                ruleMatch.matchedPrefix(),
                ruleMatch.rule().mappedDatabaseType(),
                ruleMatch.rule().supported(),
                ruleMatch.rule().unsupportedReason(),
                parts
        );
    }

    private JdbcUrlParts parseByRule(JdbcVendorRule rule, String jdbcUrl, String matchedPrefix) {
        return switch (rule.parserKind()) {
            case URI -> parseUriStyle(rule, jdbcUrl, matchedPrefix);
            case ORACLE -> parseOracleUrl(jdbcUrl);
            case SQLSERVER -> parseSqlServerUrl(jdbcUrl, matchedPrefix);
            case UNSUPPORTED -> emptyJdbcUrlParts();
        };
    }

    private JdbcUrlParts parseUriStyle(JdbcVendorRule rule, String jdbcUrl, String matchedPrefix) {
        String body = normalizeUriBody(jdbcUrl.substring(matchedPrefix.length()));
        if (!body.startsWith("//")) {
            throw new IllegalArgumentException("URI 风格 JDBC URL 缺少 // 主机段");
        }
        String hostAndPath = body.substring(2);
        int slashIndex = hostAndPath.indexOf('/');
        String authority = slashIndex >= 0 ? hostAndPath.substring(0, slashIndex) : hostAndPath;
        String pathAndParameters = slashIndex >= 0 ? hostAndPath.substring(slashIndex + 1) : "";
        ParsedPath parsedPath = splitPathAndParameters(pathAndParameters);
        HostPort hostPort = parseAuthority(authority);
        Map<String, String> parameters = parseMixedParameters(parsedPath.parameterFragment());
        String database = decode(trimToNull(parsedPath.path()));
        String schema = pickParameter(parameters, rule.schemaKeys());
        return JdbcUrlParts.builder()
                .host(hostPort.host())
                .port(hostPort.port())
                .database(database)
                .schema(trimToNull(schema))
                .serviceNameOrSid(database)
                .parameters(parameters)
                .build();
    }

    private JdbcUrlParts parseOracleUrl(String jdbcUrl) {
        String normalizedUrl = normalizeOracleJdbcUrl(jdbcUrl);
        String body = stripPrefix("jdbc:oracle:thin:@", normalizedUrl);
        if (body.startsWith("(")) {
            return parseOracleDescriptionUrl(body);
        }

        Matcher serviceMatcher = ORACLE_SERVICE_PATTERN.matcher(body);
        if (serviceMatcher.matches()) {
            return JdbcUrlParts.builder()
                    .host(trimToNull(serviceMatcher.group("host")))
                    .port(parseInteger(serviceMatcher.group("port")))
                    .database(trimToNull(serviceMatcher.group("database")))
                    .serviceNameOrSid(trimToNull(serviceMatcher.group("database")))
                    .sidMode(false)
                    .parameters(parseMixedParameters(serviceMatcher.group("rest")))
                    .build();
        }

        Matcher sidMatcher = ORACLE_SID_PATTERN.matcher(body);
        if (sidMatcher.matches()) {
            return JdbcUrlParts.builder()
                    .host(trimToNull(sidMatcher.group("host")))
                    .port(parseInteger(sidMatcher.group("port")))
                    .database(trimToNull(sidMatcher.group("database")))
                    .serviceNameOrSid(trimToNull(sidMatcher.group("database")))
                    .sidMode(true)
                    .parameters(parseMixedParameters(sidMatcher.group("rest")))
                    .build();
        }
        throw new IllegalArgumentException("Oracle JDBC URL 仅支持 service name、SID 或 DESCRIPTION 三种常见形式");
    }

    private JdbcUrlParts parseOracleDescriptionUrl(String body) {
        int descriptionEnd = findBalancedDescriptionEnd(body);
        String description = descriptionEnd >= 0 ? body.substring(0, descriptionEnd + 1) : body;
        String rest = descriptionEnd >= 0 && descriptionEnd + 1 < body.length() ? body.substring(descriptionEnd + 1) : "";
        String serviceName = trimToNull(findPatternValue(ORACLE_DESCRIPTION_SERVICE_PATTERN, description));
        String sid = trimToNull(findPatternValue(ORACLE_DESCRIPTION_SID_PATTERN, description));
        String database = firstNonBlank(serviceName, sid);
        return JdbcUrlParts.builder()
                .host(firstNonBlank(findPatternValue(ORACLE_DESCRIPTION_HOST_PATTERN, description)))
                .port(parseInteger(findPatternValue(ORACLE_DESCRIPTION_PORT_PATTERN, description)))
                .database(database)
                .serviceNameOrSid(database)
                .sidMode(StrUtil.isBlank(serviceName))
                .parameters(parseMixedParameters(rest))
                .build();
    }

    private JdbcUrlParts parseSqlServerUrl(String jdbcUrl, String matchedPrefix) {
        String body = jdbcUrl.substring(matchedPrefix.length()).replaceFirst("^//", "");
        String[] segments = body.split(";");
        HostPort hostPort = parseAuthority(segments.length > 0 ? segments[0] : "");
        Map<String, String> parameters = parseKeyValueSegments(List.of(segments).subList(Math.min(1, segments.length), segments.length));
        String database = pickParameter(parameters, List.of("databaseName", "database"));
        return JdbcUrlParts.builder()
                .host(hostPort.host())
                .port(hostPort.port())
                .database(database)
                .schema(pickParameter(parameters, List.of("schema")))
                .serviceNameOrSid(database)
                .parameters(parameters)
                .build();
    }

    private JdbcRuleMatch matchRule(String jdbcUrl) {
        JdbcRuleMatch bestMatch = null;
        for (JdbcVendorRule rule : JDBC_VENDOR_RULES) {
            for (String prefix : rule.prefixes()) {
                if (jdbcUrl.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                    if (bestMatch == null || prefix.length() > bestMatch.matchedPrefix().length()) {
                        bestMatch = new JdbcRuleMatch(rule, prefix);
                    }
                }
            }
        }
        return bestMatch;
    }

    private String unwrapJdbcUrl(String jdbcUrl) {
        String current = jdbcUrl;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String wrapperPrefix : JDBC_WRAPPER_PREFIXES) {
                if (current.toLowerCase(Locale.ROOT).startsWith(wrapperPrefix)) {
                    current = JDBC_PROTOCOL + current.substring(wrapperPrefix.length());
                    changed = true;
                    break;
                }
            }
        }
        return current;
    }

    private String normalizeOracleJdbcUrl(String jdbcUrl) {
        if (StrUtil.startWithIgnoreCase(jdbcUrl, "jdbc:alibaba:oracle:")) {
            return "jdbc:oracle:thin:@" + jdbcUrl.substring("jdbc:alibaba:oracle:".length());
        }
        return jdbcUrl;
    }

    private String normalizeUriBody(String body) {
        if (StrUtil.startWith(body, "//")) {
            return body;
        }
        int transportSeparator = body.indexOf("://");
        return transportSeparator >= 0 ? body.substring(transportSeparator + 1) : body;
    }

    private ParsedPath splitPathAndParameters(String pathAndParameters) {
        int queryIndex = pathAndParameters.indexOf('?');
        int semicolonIndex = pathAndParameters.indexOf(';');
        int splitIndex = -1;
        if (queryIndex >= 0 && semicolonIndex >= 0) {
            splitIndex = Math.min(queryIndex, semicolonIndex);
        } else if (queryIndex >= 0) {
            splitIndex = queryIndex;
        } else if (semicolonIndex >= 0) {
            splitIndex = semicolonIndex;
        }
        if (splitIndex < 0) {
            return new ParsedPath(pathAndParameters, "");
        }
        return new ParsedPath(pathAndParameters.substring(0, splitIndex), pathAndParameters.substring(splitIndex));
    }

    private HostPort parseAuthority(String authority) {
        String node = firstAuthorityNode(authority);
        if (StrUtil.isBlank(node)) {
            return new HostPort(null, null);
        }
        String normalized = trimToNull(node);
        if (normalized == null) {
            return new HostPort(null, null);
        }
        if (normalized.startsWith("[")) {
            int end = normalized.indexOf(']');
            String host = end > 0 ? normalized.substring(1, end) : normalized;
            Integer port = end > 0 && end + 1 < normalized.length() && normalized.charAt(end + 1) == ':'
                    ? parseInteger(normalized.substring(end + 2))
                    : null;
            return new HostPort(trimToNull(host), port);
        }
        int lastColon = normalized.lastIndexOf(':');
        if (lastColon > 0 && normalized.indexOf(':') == lastColon) {
            return new HostPort(trimToNull(normalized.substring(0, lastColon)),
                    parseInteger(normalized.substring(lastColon + 1)));
        }
        return new HostPort(trimToNull(normalized), null);
    }

    private String firstAuthorityNode(String authority) {
        if (StrUtil.isBlank(authority)) {
            return null;
        }
        int bracketDepth = 0;
        for (int index = 0; index < authority.length(); index++) {
            char current = authority.charAt(index);
            if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
            } else if (current == ',' && bracketDepth == 0) {
                return authority.substring(0, index);
            }
        }
        return authority;
    }

    private int findBalancedDescriptionEnd(String body) {
        int depth = 0;
        for (int index = 0; index < body.length(); index++) {
            char current = body.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private String findPatternValue(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(StrUtil.blankToDefault(text, ""));
        return matcher.find() ? trimToNull(matcher.group("value")) : null;
    }

    private Map<String, String> parseMixedParameters(String fragment) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (StrUtil.isBlank(fragment)) {
            return parameters;
        }
        String normalized = fragment.trim();
        if (normalized.startsWith("?") || normalized.startsWith(";")) {
            normalized = normalized.substring(1);
        }
        if (StrUtil.isBlank(normalized)) {
            return parameters;
        }
        String[] segments = normalized.split("[&;]");
        return parseKeyValueSegments(List.of(segments));
    }

    private Map<String, String> parseKeyValueSegments(List<String> segments) {
        Map<String, String> parameters = new LinkedHashMap<>();
        for (String rawSegment : segments) {
            String segment = trimToNull(rawSegment);
            if (segment == null) {
                continue;
            }
            int equalIndex = segment.indexOf('=');
            if (equalIndex < 0) {
                parameters.put(decode(segment), "");
                continue;
            }
            parameters.put(decode(segment.substring(0, equalIndex)),
                    decode(segment.substring(equalIndex + 1)));
        }
        return parameters;
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

    private String stripPrefix(String prefix, String jdbcUrl) {
        if (!StrUtil.startWithIgnoreCase(jdbcUrl, prefix)) {
            throw new IllegalArgumentException("JDBC URL 前缀不匹配");
        }
        return jdbcUrl.substring(prefix.length());
    }

    private String decode(String value) {
        return URLDecoder.decode(StrUtil.blankToDefault(value, ""), StandardCharsets.UTF_8);
    }

    private Integer parseInteger(String value) {
        return StrUtil.isBlank(value) ? null : Integer.parseInt(value.trim());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StrUtil.trimToNull(value);
    }

    private JdbcUrlParts emptyJdbcUrlParts() {
        return JdbcUrlParts.builder()
                .parameters(new LinkedHashMap<>())
                .build();
    }

    private enum JdbcParserKind {
        URI,
        ORACLE,
        SQLSERVER,
        UNSUPPORTED
    }

    private record JdbcVendorRule(String vendorCode,
                                  List<String> prefixes,
                                  JdbcParserKind parserKind,
                                  DatabaseType mappedDatabaseType,
                                  boolean supported,
                                  List<String> schemaKeys,
                                  String unsupportedReason) {

        private static JdbcVendorRule supported(String vendorCode,
                                                List<String> prefixes,
                                                JdbcParserKind parserKind,
                                                DatabaseType mappedDatabaseType) {
            return supported(vendorCode, prefixes, parserKind, mappedDatabaseType, List.of());
        }

        private static JdbcVendorRule supported(String vendorCode,
                                                List<String> prefixes,
                                                JdbcParserKind parserKind,
                                                DatabaseType mappedDatabaseType,
                                                List<String> schemaKeys) {
            return new JdbcVendorRule(vendorCode, prefixes, parserKind, mappedDatabaseType, true, schemaKeys, null);
        }

        private static JdbcVendorRule unsupported(String vendorCode,
                                                  List<String> prefixes,
                                                  JdbcParserKind parserKind,
                                                  String unsupportedReason) {
            return new JdbcVendorRule(vendorCode, prefixes, parserKind, null, false, List.of(), unsupportedReason);
        }
    }

    private record JdbcRuleMatch(JdbcVendorRule rule, String matchedPrefix) {
    }

    private record ParsedPath(String path, String parameterFragment) {
    }

    private record HostPort(String host, Integer port) {
    }
}
