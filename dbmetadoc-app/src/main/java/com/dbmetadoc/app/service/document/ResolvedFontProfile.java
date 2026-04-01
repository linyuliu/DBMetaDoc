package com.dbmetadoc.app.service.document;

import com.dbmetadoc.generator.PdfFontResource;
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
    String pdfTitleFontCss;
    String pdfBodyFontCss;
    String pdfMonoFontCss;
    List<String> pdfFontFiles;
    List<PdfFontResource> pdfFontResources;
}


