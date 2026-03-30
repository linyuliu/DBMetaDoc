package com.dbmetadoc.generator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档字体渲染配置。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FontRenderProfile {

    private String code;
    private String label;
    private String titleFont;
    private String bodyFont;
    private String monoFont;
    private String titleFontCss;
    private String bodyFontCss;
    private String monoFontCss;
    private List<String> pdfFontFiles;

    public void setPdfFontFiles(List<String> pdfFontFiles) {
        this.pdfFontFiles = pdfFontFiles == null ? null : new ArrayList<>(pdfFontFiles);
    }
}
