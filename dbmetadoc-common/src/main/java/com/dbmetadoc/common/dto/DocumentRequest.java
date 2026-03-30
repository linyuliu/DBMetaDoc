package com.dbmetadoc.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 文档请求参数。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentRequest extends ConnectionRequest {

    @NotBlank(message = "文档格式不能为空")
    private String format;

    private String title;

    private List<String> selectedTableKeys;

    private List<String> exportSections;

    private String fontPreset;
}


