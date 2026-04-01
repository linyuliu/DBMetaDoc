package com.dbmetadoc.app.service.jdbc;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.db.core.DatabaseType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 为元数据提取补齐数据库驱动所需的 JDBC 参数。
 * <p>
 * 这里只构造“最终有效连接参数”，不会回写用户保存的原始 URL。
 * </p>
 *
 * @author mumu
 * @date 2026-04-01
 */
public final class JdbcMetadataParameterAugmenter {

    /**
     * MyBatis-Flex FAQ 明确指出：
     * MySQL 注释读取需要 {@code useInformationSchema=true}，
     * Oracle 注释读取需要 {@code remarksReporting=true}。
     */
    private static final Map<DatabaseType, List<JdbcParameterSuggestion>> METADATA_PARAMETER_RULES = Map.of(
            DatabaseType.MYSQL, List.of(new JdbcParameterSuggestion("useInformationSchema", "true")),
            DatabaseType.ORACLE, List.of(new JdbcParameterSuggestion("remarksReporting", "true"))
    );

    /**
     * 在不覆盖用户显式参数的前提下，返回补齐后的 JDBC 参数副本。
     */
    public Map<String, String> augment(DatabaseType databaseType, Map<String, String> parsedParameters) {
        Map<String, String> effectiveParameters = new LinkedHashMap<>();
        if (parsedParameters != null) {
            effectiveParameters.putAll(parsedParameters);
        }
        for (JdbcParameterSuggestion suggestion : METADATA_PARAMETER_RULES.getOrDefault(databaseType, List.of())) {
            putParameterIfMissing(effectiveParameters, suggestion.key(), suggestion.value());
        }
        return effectiveParameters;
    }

    private void putParameterIfMissing(Map<String, String> parameters, String key, String value) {
        if (StrUtil.isBlank(key) || containsParameterIgnoreCase(parameters, key)) {
            return;
        }
        parameters.put(key, value);
    }

    private boolean containsParameterIgnoreCase(Map<String, String> parameters, String key) {
        if (parameters == null || parameters.isEmpty()) {
            return false;
        }
        for (String currentKey : parameters.keySet()) {
            if (StrUtil.equalsIgnoreCase(currentKey, key)) {
                return true;
            }
        }
        return false;
    }

    private record JdbcParameterSuggestion(String key, String value) {
    }
}
