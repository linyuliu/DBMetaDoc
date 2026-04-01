package com.dbmetadoc.app.service.jdbc;

import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.JdbcUrlParts;

import java.util.LinkedHashMap;

/**
 * JDBC URL 解析后的标准化结果。
 *
 * @author mumu
 * @date 2026-04-01
 */
public record ParsedJdbcUrl(boolean recognized,
                            String vendorCode,
                            String matchedPrefix,
                            DatabaseType mappedDatabaseType,
                            boolean supported,
                            String unsupportedReason,
                            JdbcUrlParts parts) {

    public ParsedJdbcUrl {
        parts = parts == null
                ? JdbcUrlParts.builder().parameters(new LinkedHashMap<>()).build()
                : parts;
    }

    /**
     * 返回“未识别”的空解析结果，供上层继续走结构化字段兜底。
     */
    public static ParsedJdbcUrl unknown() {
        return new ParsedJdbcUrl(false, null, null, null, true, null,
                JdbcUrlParts.builder().parameters(new LinkedHashMap<>()).build());
    }
}
