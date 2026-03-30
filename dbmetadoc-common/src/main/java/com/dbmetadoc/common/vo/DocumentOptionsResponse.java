package com.dbmetadoc.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档导出选项响应。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentOptionsResponse {

    private String defaultFontPreset;
    private List<FontPresetResponse> fontPresets;
    private List<ExportSectionResponse> exportSections;
    private List<String> defaultExportSections;
}


