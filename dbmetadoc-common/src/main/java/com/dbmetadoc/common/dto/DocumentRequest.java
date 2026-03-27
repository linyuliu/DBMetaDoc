package com.dbmetadoc.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentRequest extends ConnectionRequest {

    @NotBlank(message = "文档格式不能为空")
    private String format;

    private String title;
}
