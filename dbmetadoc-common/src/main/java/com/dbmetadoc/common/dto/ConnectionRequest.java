package com.dbmetadoc.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通用连接请求参数。
 * <p>
 * 结构化字段优先于 jdbcUrl；当结构化字段缺失时，服务层会尝试从 jdbcUrl 中解析并补齐。
 * </p>
 */
@Data
public class ConnectionRequest {

    private Long datasourceId;

    @NotBlank(message = "数据库类型不能为空")
    private String dbType;

    /**
     * 可选的 JDBC URL 辅助输入。
     */
    private String jdbcUrl;

    private String host;

    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号不能超过65535")
    private Integer port;

    private String database;

    private String schema;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Boolean useCache;

    private Boolean forceRefresh;
}
