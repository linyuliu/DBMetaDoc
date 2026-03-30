package com.dbmetadoc.db.core;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统内置支持的数据库类型。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Getter
@AllArgsConstructor
public enum DatabaseType {

    MYSQL("MySQL", 3306, "com.mysql.cj.jdbc.Driver", "SELECT 1", false, true, false, false,
            true, false, true, "SHOW -> information_schema -> JDBC Metadata") {
        @Override
        protected String doBuildJdbcUrl(DatabaseConnectionInfo connectionInfo) {
            return String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    connectionInfo.getHost(), connectionInfo.getEffectivePort(), connectionInfo.getDatabase());
        }
    },
    POSTGRESQL("PostgreSQL", 5432, "org.postgresql.Driver", "SELECT 1", false, false, true, false,
            true, true, true, "pg_catalog -> information_schema -> JDBC Metadata") {
        @Override
        protected String doBuildJdbcUrl(DatabaseConnectionInfo connectionInfo) {
            StringBuilder builder = new StringBuilder(String.format("jdbc:postgresql://%s:%d/%s",
                    connectionInfo.getHost(), connectionInfo.getEffectivePort(), connectionInfo.getDatabase()));
            if (StrUtil.isNotBlank(connectionInfo.getSchema())) {
                builder.append("?currentSchema=").append(connectionInfo.getSchema());
            }
            return builder.toString();
        }
    },
    ORACLE("Oracle", 1521, "oracle.jdbc.OracleDriver", "SELECT 1 FROM DUAL", false, false, false, true,
            true, true, true, "ALL_* -> USER_* -> JDBC Metadata") {
        @Override
        protected String doBuildJdbcUrl(DatabaseConnectionInfo connectionInfo) {
            if (connectionInfo.isSidMode()) {
                return String.format("jdbc:oracle:thin:@%s:%d:%s",
                        connectionInfo.getHost(), connectionInfo.getEffectivePort(), connectionInfo.getDatabase());
            }
            return String.format("jdbc:oracle:thin:@//%s:%d/%s",
                    connectionInfo.getHost(), connectionInfo.getEffectivePort(), connectionInfo.getDatabase());
        }
    },
    KINGBASE("KingbaseES", 54321, "com.kingbase8.Driver", "SELECT 1", true, true, true, true,
            true, true, true, "模式识别 -> PG 目录 / Oracle 字典 / Kingbase MySQL 信息模式") {
        @Override
        protected String doBuildJdbcUrl(DatabaseConnectionInfo connectionInfo) {
            StringBuilder builder = new StringBuilder(String.format("jdbc:kingbase8://%s:%d/%s",
                    connectionInfo.getHost(), connectionInfo.getEffectivePort(), connectionInfo.getDatabase()));
            if (StrUtil.isNotBlank(connectionInfo.getSchema())) {
                builder.append("?currentSchema=").append(connectionInfo.getSchema());
            }
            return builder.toString();
        }
    },
    DAMENG("Dameng", 5236, "dm.jdbc.driver.DmDriver", "SELECT 1 FROM DUAL", true, false, false, true,
            true, true, true, "系统视图 -> JDBC Metadata") {
        @Override
        protected String doBuildJdbcUrl(DatabaseConnectionInfo connectionInfo) {
            StringBuilder builder = new StringBuilder(String.format("jdbc:dm://%s:%d",
                    connectionInfo.getHost(), connectionInfo.getEffectivePort()));
            if (StrUtil.isNotBlank(connectionInfo.getDatabase())) {
                builder.append("/").append(connectionInfo.getDatabase());
            }
            if (StrUtil.isNotBlank(connectionInfo.getSchema())) {
                builder.append(builder.indexOf("?") > -1 ? "&" : "?").append("schema=").append(connectionInfo.getSchema());
            }
            return builder.toString();
        }
    };

    private final String label;
    private final int defaultPort;
    private final String driverClass;
    private final String testSql;
    private final boolean domestic;
    private final boolean mysqlLike;
    private final boolean pgLike;
    private final boolean oracleLike;
    private final boolean supportsDatabase;
    private final boolean supportsSchema;
    private final boolean supportsJdbcUrl;
    private final String metadataStrategy;

    /**
     * 构造 JDBC 连接串。
     */
    protected abstract String doBuildJdbcUrl(DatabaseConnectionInfo connectionInfo);

    public String buildJdbcUrl(DatabaseConnectionInfo connectionInfo) {
        String jdbcUrl = doBuildJdbcUrl(connectionInfo);
        if (connectionInfo.getJdbcParameters() == null || connectionInfo.getJdbcParameters().isEmpty()) {
            return jdbcUrl;
        }
        StringBuilder builder = new StringBuilder(jdbcUrl);
        boolean hasQuery = jdbcUrl.contains("?");
        for (var entry : connectionInfo.getJdbcParameters().entrySet()) {
            if (StrUtil.isBlank(entry.getKey()) || StrUtil.isBlank(entry.getValue())) {
                continue;
            }
            if (isReservedParameter(entry.getKey(), connectionInfo)) {
                continue;
            }
            builder.append(hasQuery ? "&" : "?")
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
            hasQuery = true;
        }
        return builder.toString();
    }

    public DriverDescriptor toDescriptor() {
        return DriverDescriptor.builder()
                .type(this)
                .label(label)
                .defaultPort(defaultPort)
                .driverClass(driverClass)
                .testSql(testSql)
                .domestic(domestic)
                .mysqlLike(mysqlLike)
                .pgLike(pgLike)
                .oracleLike(oracleLike)
                .supportsDatabase(supportsDatabase)
                .supportsSchema(supportsSchema)
                .supportsJdbcUrl(supportsJdbcUrl)
                .metadataStrategy(metadataStrategy)
                .build();
    }

    private boolean isReservedParameter(String parameterName, DatabaseConnectionInfo connectionInfo) {
        if (connectionInfo == null || StrUtil.isBlank(parameterName)) {
            return false;
        }
        String lower = parameterName.toLowerCase();
        if ((this == POSTGRESQL || this == KINGBASE) && "currentschema".equals(lower) && StrUtil.isNotBlank(connectionInfo.getSchema())) {
            return true;
        }
        return this == DAMENG && "schema".equals(lower) && StrUtil.isNotBlank(connectionInfo.getSchema());
    }

    public static DatabaseType fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ResultCode.UNSUPPORTED_DATABASE, "数据库类型不能为空");
        }
        for (DatabaseType type : values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new BusinessException(ResultCode.UNSUPPORTED_DATABASE, "暂不支持的数据库类型: " + code);
    }
}


