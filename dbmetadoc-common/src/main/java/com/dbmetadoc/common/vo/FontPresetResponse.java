package com.dbmetadoc.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字体预设响应。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FontPresetResponse {

    private String code;
    private String label;
    private String titleFont;
    private String bodyFont;
    private String monoFont;
}


