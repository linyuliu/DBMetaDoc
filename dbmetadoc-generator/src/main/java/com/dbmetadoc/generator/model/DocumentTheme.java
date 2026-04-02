package com.dbmetadoc.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档主题配置。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTheme {

    private String titleFont;
    private String bodyFont;
    private String monoFont;
    private String titleFontCss;
    private String bodyFontCss;
    private String monoFontCss;
    private String symbolFontCss;
    private String fontFaceCss;
    private String primaryColor;
    private String primaryDarkColor;
    private String textColor;
    private String labelColor;
    private String mutedColor;
    private String borderColor;
    private String headerBorderColor;
    private String stripeColor;
    private String headerColor;
    private String softColor;
    private String coverTitleSize;
    private String sectionTitleSize;
    private String subTitleSize;
    private String bodyFontSize;
    private String lineHeight;
    private String pageMargin;
}
