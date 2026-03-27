package com.dbmetadoc.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConnectionRequest {

    private Long datasourceId;

    @NotBlank(message = "数据库类型不能为空")
    private String dbType;

    @NotBlank(message = "主机地址不能为空")
    private String host;

    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号不能超过65535")
    private Integer port;

    @NotBlank(message = "数据库名不能为空")
    private String database;
    private String schema;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
    private Boolean useCache;
    private Boolean forceRefresh;
}
