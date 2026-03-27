package com.dbmetadoc.db.kingbase;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.db.core.AbstractMysqlLikeTableExtractor;
import com.dbmetadoc.db.core.AbstractOracleLikeTableExtractor;
import com.dbmetadoc.db.core.AbstractPgLikeTableExtractor;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.DriverDescriptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Kingbase 元数据提取实现。
 */
@Slf4j
public class KingbaseMetadataExtractor extends AbstractPgLikeTableExtractor {

    private final KingbaseMysqlModeExtractor mysqlModeExtractor = new KingbaseMysqlModeExtractor();
    private final KingbaseOracleModeExtractor oracleModeExtractor = new KingbaseOracleModeExtractor();

    @Getter
    enum CompatibilityMode {
        PG("pg", "PostgreSQL 兼容模式"),
        ORACLE("oracle", "Oracle 兼容模式"),
        MYSQL("mysql", "MySQL 兼容模式"),
        MSSQL("mssql", "SQL Server 兼容模式");

        private final String code;
        private final String description;

        CompatibilityMode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        static CompatibilityMode fromCode(String code) {
            if (StrUtil.isBlank(code)) {
                return PG;
            }
            String normalized = code.trim().toLowerCase();
            for (CompatibilityMode mode : values()) {
                if (Objects.equals(mode.code, normalized)) {
                    return mode;
                }
            }
            return PG;
        }
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.KINGBASE;
    }

    @Override
    public DriverDescriptor getDriverDescriptor() {
        DriverDescriptor descriptor = DatabaseType.KINGBASE.toDescriptor();
        descriptor.setTestSql("自动识别兼容模式：PG/MySQL/MSSQL 使用 SELECT 1，Oracle 使用 SELECT 1 FROM DUAL");
        descriptor.setMetadataStrategy("模式探测 -> PG 目录 / Oracle 字典 / MySQL information_schema+SHOW");
        return descriptor;
    }

    @Override
    public String resolveTestSql(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        CompatibilityMode mode = detectCompatibilityMode(connection);
        return mode == CompatibilityMode.ORACLE ? "SELECT 1 FROM DUAL" : "SELECT 1";
    }

    @Override
    public DatabaseInfo extract(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
        CompatibilityMode mode = detectCompatibilityMode(connection);
        log.info("检测到 Kingbase 兼容模式：{}，数据库：{}，Schema：{}",
                mode.getDescription(), connectionInfo.getDatabase(), connectionInfo.getSchema());

        DatabaseInfo databaseInfo = switch (mode) {
            case ORACLE -> oracleModeExtractor.extract(connection, adaptOracleConnectionInfo(connectionInfo));
            case MYSQL -> mysqlModeExtractor.extract(connection, adaptMysqlConnectionInfo(connectionInfo));
            case PG, MSSQL -> super.extract(connection, adaptPgConnectionInfo(connectionInfo));
        };
        databaseInfo.setVersion(appendMode(databaseInfo.getVersion(), mode));
        databaseInfo.setType(DatabaseType.KINGBASE.name());
        return databaseInfo;
    }

    CompatibilityMode detectCompatibilityMode(Connection connection) throws SQLException {
        String mode = querySingleString(connection, "SHOW database_mode", 1);
        if (StrUtil.isBlank(mode)) {
            mode = querySingleString(connection, "SELECT setting FROM pg_settings WHERE name = 'database_mode'", "setting");
        }
        if (StrUtil.isBlank(mode)) {
            mode = querySingleString(connection, "SELECT current_setting('database_mode')", 1);
        }
        return CompatibilityMode.fromCode(mode);
    }

    private DatabaseConnectionInfo adaptPgConnectionInfo(DatabaseConnectionInfo connectionInfo) {
        connectionInfo.setSchema(StrUtil.blankToDefault(connectionInfo.getSchema(), "public"));
        connectionInfo.setSchemaName(connectionInfo.getSchema());
        return connectionInfo;
    }

    private DatabaseConnectionInfo adaptOracleConnectionInfo(DatabaseConnectionInfo connectionInfo) {
        connectionInfo.setSchema(StrUtil.blankToDefault(connectionInfo.getSchema(), connectionInfo.getUsername()).toUpperCase());
        connectionInfo.setSchemaName(connectionInfo.getSchema());
        return connectionInfo;
    }

    private DatabaseConnectionInfo adaptMysqlConnectionInfo(DatabaseConnectionInfo connectionInfo) {
        connectionInfo.setSchema(null);
        connectionInfo.setSchemaName(null);
        return connectionInfo;
    }

    private String appendMode(String version, CompatibilityMode mode) {
        return StrUtil.format("{} [{}]", StrUtil.blankToDefault(version, "unknown"), mode.getDescription());
    }

    private String querySingleString(Connection connection, String sql, int index) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(index);
            }
        } catch (SQLException ex) {
            log.debug("执行 Kingbase 兼容模式探测 SQL 失败，SQL：{}，原因：{}", sql, ex.getMessage());
        }
        return null;
    }

    private String querySingleString(Connection connection, String sql, String columnName) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(columnName);
            }
        } catch (SQLException ex) {
            log.debug("执行 Kingbase 兼容模式探测 SQL 失败，SQL：{}，原因：{}", sql, ex.getMessage());
        }
        return null;
    }

    private static final class KingbaseOracleModeExtractor extends AbstractOracleLikeTableExtractor {

        @Override
        public DatabaseType getDatabaseType() {
            return DatabaseType.KINGBASE;
        }
    }

    private static final class KingbaseMysqlModeExtractor extends AbstractMysqlLikeTableExtractor {

        @Override
        public DatabaseType getDatabaseType() {
            return DatabaseType.KINGBASE;
        }

        @Override
        public DatabaseInfo extract(Connection connection, DatabaseConnectionInfo connectionInfo) throws SQLException {
            try {
                log.info("Kingbase MySQL 模式优先使用 information_schema 提取，数据库：{}", connectionInfo.getDatabase());
                return extractByInformationSchema(connection, connectionInfo);
            } catch (SQLException ex) {
                log.warn("Kingbase MySQL 模式 information_schema 提取失败，尝试 SHOW，原因：{}", ex.getMessage());
            }
            return super.extract(connection, connectionInfo);
        }
    }
}
