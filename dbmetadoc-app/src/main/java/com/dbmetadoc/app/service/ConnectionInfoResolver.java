package com.dbmetadoc.app.service;

import com.dbmetadoc.app.service.jdbc.JdbcConnectionFieldResolver;
import com.dbmetadoc.app.service.jdbc.JdbcConnectionValidator;
import com.dbmetadoc.app.service.jdbc.JdbcResolvedFields;
import com.dbmetadoc.app.service.jdbc.JdbcUrlParser;
import com.dbmetadoc.app.service.jdbc.ParsedJdbcUrl;
import com.dbmetadoc.common.dto.ConnectionRequest;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.ResolvedConnectionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 连接参数归一化服务。
 * <p>
 * 当前职责只保留编排：
 * parser 负责 JDBC URL 识别，
 * field resolver 负责字段优先级与参数补强，
 * validator 负责冲突和必填项校验。
 * </p>
 *
 * @author mumu
 * @date 2026-04-01
 */
@Slf4j
@Component
public class ConnectionInfoResolver {

    private final JdbcUrlParser jdbcUrlParser = new JdbcUrlParser();
    private final JdbcConnectionFieldResolver jdbcConnectionFieldResolver = new JdbcConnectionFieldResolver();
    private final JdbcConnectionValidator jdbcConnectionValidator = new JdbcConnectionValidator();

    public ResolvedConnectionInfo resolve(ConnectionRequest request) {
        ParsedJdbcUrl parsedJdbcUrl = jdbcUrlParser.parse(request.getJdbcUrl());
        DatabaseType databaseType = jdbcConnectionValidator.resolveDatabaseType(request, parsedJdbcUrl);
        JdbcResolvedFields resolvedFields = jdbcConnectionFieldResolver.resolve(request, parsedJdbcUrl, databaseType);

        jdbcConnectionValidator.validateResolvedTarget(databaseType, resolvedFields);

        DatabaseConnectionInfo connectionInfo = resolvedFields.toDatabaseConnectionInfo(request, databaseType);
        connectionInfo.setResolvedJdbcUrl(databaseType.buildJdbcUrl(connectionInfo));
        jdbcConnectionValidator.validateCredentials(connectionInfo);

        ResolvedConnectionInfo resolved = resolvedFields.toResolvedConnectionInfo(
                request, databaseType, connectionInfo.getResolvedJdbcUrl());

        log.info("连接参数归一化完成，数据库类型：{}，主机：{}，端口：{}，数据库：{}，Schema：{}，是否提供JDBC URL：{}",
                databaseType.name(),
                resolved.getHost(),
                connectionInfo.getEffectivePort(),
                resolved.getDatabase(),
                resolved.getSchema(),
                request.getJdbcUrl() != null && !request.getJdbcUrl().isBlank());
        log.debug("归一化后的 JDBC URL：{}", connectionInfo.getResolvedJdbcUrl());
        return resolved;
    }
}
