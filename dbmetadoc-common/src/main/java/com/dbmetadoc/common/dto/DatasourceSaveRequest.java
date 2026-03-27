package com.dbmetadoc.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DatasourceSaveRequest extends ConnectionRequest {

    private Long id;

    @NotBlank(message = "数据源名称不能为空")
    private String name;

    private String driverClass;
    private String remark;
    private Boolean enabled;
}
