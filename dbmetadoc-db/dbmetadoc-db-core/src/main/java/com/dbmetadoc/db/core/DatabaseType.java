package com.dbmetadoc.db.core;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统内置支持的数据库类型。
 */
@Getter
@AllArgsConstructor
public enum DatabaseType {

    MYSQL("MySQL", 3306, "com.mysql.cj.jdbc.Driver", "SELECT 1", false, true, false, false) {
        @Override
        public String buildJdbcUrl(String host, int port, String database, String schema) {
            return String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port, database);
        }
    },
    POSTGRESQL("PostgreSQL", 5432, "org.postgresql.Driver", "SELECT 1", false, false, true, false) {
        @Override
        public String buildJdbcUrl(String host, int port, String database, String schema) {
            StringBuilder builder = new StringBuilder(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
            if (StrUtil.isNotBlank(schema)) {
                builder.append("?currentSchema=").append(schema);
            }
            return builder.toString();
        }
    },
    ORACLE("Oracle", 1521, "oracle.jdbc.OracleDriver", "SELECT 1 FROM DUAL", false, false, false, true) {
        @Override
        public String buildJdbcUrl(String host, int port, String database, String schema) {
            return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, database);
        }
    },
    KINGBASE("KingbaseES", 54321, "com.kingbase8.Driver", "SELECT 1", true, true, true, true) {
        @Override
        public String buildJdbcUrl(String host, int port, String database, String schema) {
            StringBuilder builder = new StringBuilder(String.format("jdbc:kingbase8://%s:%d/%s", host, port, database));
            if (StrUtil.isNotBlank(schema)) {
                builder.append("?currentSchema=").append(schema);
            }
            return builder.toString();
        }
    },
    DAMENG("Dameng", 5236, "dm.jdbc.driver.DmDriver", "SELECT 1 FROM DUAL", true, false, false, true) {
        @Override
        public String buildJdbcUrl(String host, int port, String database, String schema) {
            StringBuilder builder = new StringBuilder(String.format("jdbc:dm://%s:%d", host, port));
            if (StrUtil.isNotBlank(database)) {
                builder.append("/").append(database);
            }
            if (StrUtil.isNotBlank(schema)) {
                builder.append(builder.indexOf("?") > -1 ? "&" : "?").append("schema=").append(schema);
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

    /**
     * 构造 JDBC 连接串。
     */
    public abstract String buildJdbcUrl(String host, int port, String database, String schema);

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
                .build();
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
