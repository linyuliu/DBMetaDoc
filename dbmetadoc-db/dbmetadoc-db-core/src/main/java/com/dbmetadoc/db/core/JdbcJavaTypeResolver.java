package com.dbmetadoc.db.core;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.model.ColumnInfo;

import java.util.Locale;

/**
 * JDBC 列类型到 Java 类型的统一映射工具。
 * <p>
 * 映射策略遵循“尽量保守”的原则：
 * 1. 常见关系型数据库类型映射到明确的 Java 类型；
 * 2. 国产数据库或未来 OLAP 场景中识别不了的类型统一回退为 String；
 * 3. 允许各数据库提取器在公共映射之上继续覆盖。
 * </p>
 *
 * @author mumu
 * @date 2026-03-30
 */
public final class JdbcJavaTypeResolver {

    private JdbcJavaTypeResolver() {
    }

    /**
     * 通用兜底映射。
     */
    public static String resolveGeneric(ColumnInfo columnInfo) {
        String normalizedType = normalizeTypeName(columnInfo);
        if (StrUtil.isBlank(normalizedType)) {
            return "String";
        }

        if (isArrayLike(columnInfo) || isComplexLike(normalizedType)) {
            return "String";
        }
        if (matches(normalizedType, "uuid", "uniqueidentifier")) {
            return "UUID";
        }
        if (matches(normalizedType, "boolean", "bool")) {
            return "Boolean";
        }
        if (matches(normalizedType, "bit")) {
            return resolveBitJavaType(columnInfo);
        }
        if (isBinaryLike(normalizedType)) {
            return "byte[]";
        }
        if (isStringLike(normalizedType)) {
            return "String";
        }
        if (matches(normalizedType, "tinyint", "smallint", "int2")) {
            return "Integer";
        }
        if (matches(normalizedType, "integer", "int", "int4", "mediumint", "serial", "smallserial")) {
            return "Integer";
        }
        if (matches(normalizedType, "bigint", "int8", "bigserial")) {
            return isUnsigned(columnInfo) ? "BigInteger" : "Long";
        }
        if (matches(normalizedType, "decimal", "numeric", "money", "smallmoney", "number")) {
            return "BigDecimal";
        }
        if (matches(normalizedType, "real", "float4", "binary_float")) {
            return "Float";
        }
        if (matches(normalizedType, "float", "float8", "double", "double precision", "binary_double")) {
            return "Double";
        }
        if (matches(normalizedType, "date")) {
            return "LocalDate";
        }
        if (matches(normalizedType, "time")) {
            return "LocalTime";
        }
        if (matches(normalizedType, "timetz", "time with time zone")) {
            return "OffsetTime";
        }
        if (matches(normalizedType, "timestamp", "datetime", "smalldatetime", "timestamp without time zone")) {
            return "LocalDateTime";
        }
        if (matches(normalizedType, "timestamptz", "timestamp with time zone", "datetimeoffset")) {
            return "OffsetDateTime";
        }
        if (matches(normalizedType, "year")) {
            return "Integer";
        }
        return "String";
    }

    /**
     * MySQL / MySQL 兼容类型映射。
     */
    public static String resolveMysqlLike(ColumnInfo columnInfo) {
        String normalizedType = normalizeTypeName(columnInfo);
        if (matches(normalizedType, "tinyint") && extractSingleModifier(columnInfo) == 1) {
            return "Boolean";
        }
        if (matches(normalizedType, "bit") && extractSingleModifier(columnInfo) == 1) {
            return "Boolean";
        }
        if (matches(normalizedType, "json", "enum", "set")) {
            return "String";
        }
        if (matches(normalizedType, "datetime", "timestamp")) {
            return "LocalDateTime";
        }
        return resolveGeneric(columnInfo);
    }

    /**
     * PostgreSQL / PG 兼容类型映射。
     */
    public static String resolvePgLike(ColumnInfo columnInfo) {
        String normalizedType = normalizeTypeName(columnInfo);
        if (matches(normalizedType, "json", "jsonb", "xml", "interval", "inet", "cidr", "macaddr")) {
            return "String";
        }
        if (matches(normalizedType, "serial", "smallserial")) {
            return "Integer";
        }
        if (matches(normalizedType, "bigserial", "oid")) {
            return "Long";
        }
        if (matches(normalizedType, "bytea")) {
            return "byte[]";
        }
        if (matches(normalizedType, "numeric")) {
            return "BigDecimal";
        }
        return resolveGeneric(columnInfo);
    }

    /**
     * Oracle / 达梦 / Oracle 兼容类型映射。
     */
    public static String resolveOracleLike(ColumnInfo columnInfo) {
        String normalizedType = normalizeTypeName(columnInfo);
        if (matches(normalizedType, "number", "numeric", "decimal")) {
            return resolveOracleNumber(columnInfo);
        }
        if (matches(normalizedType, "date")) {
            return "LocalDateTime";
        }
        if (matches(normalizedType, "timestamp", "timestamp without time zone")) {
            return "LocalDateTime";
        }
        if (matches(normalizedType, "timestamptz", "timestamp with time zone", "timestamp with local time zone")) {
            return "OffsetDateTime";
        }
        if (matches(normalizedType, "clob", "nclob", "long", "varchar2", "nvarchar2", "nvarchar", "nchar", "rowid", "urowid")) {
            return "String";
        }
        if (matches(normalizedType, "blob", "raw", "long raw", "bfile")) {
            return "byte[]";
        }
        return resolveGeneric(columnInfo);
    }

    /**
     * SQL Server 兼容类型映射。
     */
    public static String resolveSqlServerLike(ColumnInfo columnInfo) {
        String normalizedType = normalizeTypeName(columnInfo);
        if (matches(normalizedType, "nvarchar", "nchar", "ntext", "text", "xml")) {
            return "String";
        }
        if (matches(normalizedType, "image")) {
            return "byte[]";
        }
        if (matches(normalizedType, "money", "smallmoney")) {
            return "BigDecimal";
        }
        if (matches(normalizedType, "datetimeoffset")) {
            return "OffsetDateTime";
        }
        return resolveGeneric(columnInfo);
    }

    /**
     * Kingbase 按兼容模式做类型映射。
     */
    public static String resolveKingbase(ColumnInfo columnInfo, String compatibilityMode) {
        String mode = StrUtil.blankToDefault(compatibilityMode, "pg").toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "oracle" -> resolveOracleLike(columnInfo);
            case "mysql" -> resolveMysqlLike(columnInfo);
            case "mssql" -> resolveSqlServerLike(columnInfo);
            default -> resolvePgLike(columnInfo);
        };
    }

    /**
     * 归一化数据库原始类型。
     */
    public static String normalizeTypeName(ColumnInfo columnInfo) {
        return normalizeTypeName(columnInfo == null ? null : StrUtil.blankToDefault(columnInfo.getRawType(), columnInfo.getType()));
    }

    /**
     * 归一化数据库原始类型。
     */
    public static String normalizeTypeName(String rawType) {
        if (StrUtil.isBlank(rawType)) {
            return null;
        }
        String normalized = rawType.trim()
                .replace("\"", "")
                .replace("`", "")
                .replace("[", "")
                .replace("]", "")
                .toLowerCase(Locale.ROOT);

        if (normalized.endsWith("[]")) {
            return normalized;
        }
        if (normalized.contains("timestamp with local time zone")) {
            return "timestamp with local time zone";
        }
        if (normalized.contains("timestamp with time zone") || "timestamptz".equals(normalized)) {
            return "timestamp with time zone";
        }
        if (normalized.contains("timestamp without time zone")) {
            return "timestamp without time zone";
        }
        if (normalized.contains("time with time zone") || "timetz".equals(normalized)) {
            return "time with time zone";
        }
        if (normalized.contains("time without time zone")) {
            return "time";
        }
        if (normalized.contains("(")) {
            normalized = StrUtil.subBefore(normalized, "(", false).trim();
        }
        normalized = normalized.replace(" unsigned", "").trim();
        normalized = normalized.replace(" varying", " varying").trim();

        return switch (normalized) {
            case "character varying" -> "varchar";
            case "character" -> "char";
            case "national character varying" -> "nvarchar";
            case "national character" -> "nchar";
            case "double precision" -> "double precision";
            default -> normalized;
        };
    }

    /**
     * 判断是否包含 unsigned 修饰。
     */
    public static boolean isUnsigned(ColumnInfo columnInfo) {
        String rawType = columnInfo == null ? null : StrUtil.blankToDefault(columnInfo.getRawType(), columnInfo.getType());
        return StrUtil.containsIgnoreCase(rawType, "unsigned");
    }

    private static String resolveBitJavaType(ColumnInfo columnInfo) {
        Integer modifier = extractSingleModifier(columnInfo);
        if (modifier != null && modifier == 1) {
            return "Boolean";
        }
        return "byte[]";
    }

    private static String resolveOracleNumber(ColumnInfo columnInfo) {
        Integer scale = columnInfo.getScale();
        Integer precision = firstNonNull(columnInfo.getPrecision(), columnInfo.getLength());
        if (scale != null && scale > 0) {
            return "BigDecimal";
        }
        if (precision == null || precision <= 0) {
            return "BigDecimal";
        }
        if (precision <= 9) {
            return "Integer";
        }
        if (precision <= 18) {
            return "Long";
        }
        return "BigDecimal";
    }

    private static Integer firstNonNull(Integer first, Integer second) {
        return first != null ? first : second;
    }

    private static boolean matches(String normalizedType, String... candidates) {
        if (StrUtil.isBlank(normalizedType)) {
            return false;
        }
        for (String candidate : candidates) {
            if (normalizedType.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isArrayLike(ColumnInfo columnInfo) {
        String rawType = columnInfo == null ? null : StrUtil.blankToDefault(columnInfo.getRawType(), columnInfo.getType());
        return StrUtil.endWithIgnoreCase(StrUtil.blankToDefault(rawType, ""), "[]");
    }

    private static boolean isComplexLike(String normalizedType) {
        return matches(normalizedType, "array", "map", "struct", "record", "object", "variant");
    }

    private static boolean isBinaryLike(String normalizedType) {
        return matches(normalizedType, "binary", "varbinary", "blob", "tinyblob", "mediumblob", "longblob",
                "bytea", "raw", "long raw", "image", "bfile");
    }

    private static boolean isStringLike(String normalizedType) {
        return matches(normalizedType, "char", "varchar", "varchar2", "nvarchar", "nvarchar2", "nchar",
                "tinytext", "text", "mediumtext", "longtext", "clob", "nclob", "json", "jsonb", "xml",
                "enum", "set", "citext", "inet", "cidr", "macaddr", "geometry", "geography");
    }

    private static Integer extractSingleModifier(ColumnInfo columnInfo) {
        String rawType = columnInfo == null ? null : StrUtil.blankToDefault(columnInfo.getRawType(), columnInfo.getType());
        if (StrUtil.isBlank(rawType) || !rawType.contains("(") || !rawType.contains(")")) {
            return columnInfo == null ? null : firstNonNull(columnInfo.getLength(), columnInfo.getPrecision());
        }
        String content = StrUtil.subBetween(rawType, "(", ")");
        if (StrUtil.isBlank(content)) {
            return firstNonNull(columnInfo.getLength(), columnInfo.getPrecision());
        }
        String first = StrUtil.splitTrim(content, ',').stream().findFirst().orElse(null);
        return StrUtil.isNumeric(first) ? Integer.parseInt(first) : firstNonNull(columnInfo.getLength(), columnInfo.getPrecision());
    }
}


