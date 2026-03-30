package com.dbmetadoc.app.service.document;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 已解析的字体预设。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Value
@Builder
public class ResolvedFontProfile {

    String code;
    String label;
    String titleFont;
    String bodyFont;
    String monoFont;
    String titleFontCss;
    String bodyFontCss;
    String monoFontCss;
    List<String> pdfFontFiles;
}


