package com.dbmetadoc.app.service.jdbc;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.dto.ConnectionRequest;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;

/**
 * JDBC 解析结果校验器。
 *
 * @author mumu
 * @date 2026-04-01
 */
public final class JdbcConnectionValidator {

    /**
     * 根据显式数据库类型和 JDBC URL 识别结果，确定最终数据库方言。
     */
    public DatabaseType resolveDatabaseType(ConnectionRequest request, ParsedJdbcUrl parsedJdbcUrl) {
        DatabaseType explicitType = StrUtil.isBlank(request.getDbType()) ? null : DatabaseType.fromCode(request.getDbType());
        if (parsedJdbcUrl.recognized() && !parsedJdbcUrl.supported()) {
            throw new BusinessException(ResultCode.UNSUPPORTED_DATABASE, parsedJdbcUrl.unsupportedReason());
        }
        if (explicitType != null && parsedJdbcUrl.recognized() && parsedJdbcUrl.mappedDatabaseType() != null
                && explicitType != parsedJdbcUrl.mappedDatabaseType()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    StrUtil.format("JDBC URL 识别为 {}，但当前选择的是 {}，请统一数据库类型或修正 JDBC URL",
                            parsedJdbcUrl.mappedDatabaseType().getLabel(), explicitType.getLabel()));
        }
        if (explicitType != null) {
            return explicitType;
        }
        if (parsedJdbcUrl.mappedDatabaseType() != null) {
            return parsedJdbcUrl.mappedDatabaseType();
        }
        throw new BusinessException(ResultCode.UNSUPPORTED_DATABASE, "无法从 JDBC URL 自动识别数据库类型，请显式选择数据库类型");
    }

    /**
     * 校验连接目标必填项。
     */
    public void validateResolvedTarget(DatabaseType databaseType, JdbcResolvedFields fields) {
        if (StrUtil.isBlank(fields.host())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "主机地址不能为空，未提供结构化字段且无法从 JDBC URL 中解析");
        }
        if (StrUtil.isBlank(fields.database())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    StrUtil.format("{} 的数据库/服务名不能为空，未提供结构化字段且无法从 JDBC URL 中解析", databaseType.getLabel()));
        }
    }

    /**
     * 校验最终连接认证信息。
     */
    public void validateCredentials(DatabaseConnectionInfo connectionInfo) {
        if (StrUtil.isBlank(connectionInfo.getUsername())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名不能为空");
        }
        if (StrUtil.isBlank(connectionInfo.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空，若使用模板密码请勾选“使用已保存密码”");
        }
    }
}
