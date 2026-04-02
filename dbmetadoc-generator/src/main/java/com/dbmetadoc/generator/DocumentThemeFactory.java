package com.dbmetadoc.generator;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.generator.model.DocumentTheme;

/**
 * 文档主题工厂。
 *
 * @author mumu
 * @date 2026-03-30
 */
public final class DocumentThemeFactory {

    private static final String DEFAULT_TITLE_FONT = "Source Han Sans CN";
    private static final String DEFAULT_BODY_FONT = "Source Han Sans CN";
    private static final String DEFAULT_MONO_FONT = "JetBrains Mono";
    private static final String DEFAULT_TITLE_FONT_CSS = "\"SF Pro Display\", \"PingFang SC\", \"HarmonyOS Sans SC\", \"MiSans\", \"Source Han Sans CN\", \"Noto Sans SC\", \"Microsoft YaHei\", sans-serif";
    private static final String DEFAULT_BODY_FONT_CSS = "\"SF Pro Text\", \"PingFang SC\", \"HarmonyOS Sans SC\", \"MiSans\", \"Source Han Sans CN\", \"Noto Sans SC\", \"Microsoft YaHei\", sans-serif";
    private static final String DEFAULT_MONO_FONT_CSS = "\"JetBrains Mono\", \"SF Mono\", \"Cascadia Mono\", \"Consolas\", monospace";
    private static final String DEFAULT_SYMBOL_FONT_CSS = "\"PingFang SC\", \"HarmonyOS Sans SC\", \"MiSans\", \"Noto Sans SC\", \"Source Han Sans CN\", \"Segoe UI Symbol\", \"Segoe UI Emoji\", sans-serif";

    private DocumentThemeFactory() {
    }

    public static DocumentTheme create(FontRenderProfile fontRenderProfile) {
        return create(fontRenderProfile, DocumentRenderTarget.HTML_PREVIEW);
    }

    public static DocumentTheme create(FontRenderProfile fontRenderProfile, DocumentRenderTarget renderTarget) {
        return DocumentTheme.builder()
                .titleFont(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getTitleFont(), DEFAULT_TITLE_FONT))
                .bodyFont(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getBodyFont(), DEFAULT_BODY_FONT))
                .monoFont(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getMonoFont(), DEFAULT_MONO_FONT))
                .titleFontCss(resolveFontCss(fontRenderProfile, renderTarget,
                        FontRenderProfile::getPdfTitleFontCss,
                        FontRenderProfile::getTitleFontCss,
                        DEFAULT_TITLE_FONT_CSS))
                .bodyFontCss(resolveFontCss(fontRenderProfile, renderTarget,
                        FontRenderProfile::getPdfBodyFontCss,
                        FontRenderProfile::getBodyFontCss,
                        DEFAULT_BODY_FONT_CSS))
                .monoFontCss(resolveFontCss(fontRenderProfile, renderTarget,
                        FontRenderProfile::getPdfMonoFontCss,
                        FontRenderProfile::getMonoFontCss,
                        DEFAULT_MONO_FONT_CSS))
                .symbolFontCss(resolveFontCss(fontRenderProfile, renderTarget,
                        FontRenderProfile::getPdfSymbolFontCss,
                        FontRenderProfile::getSymbolFontCss,
                        DEFAULT_SYMBOL_FONT_CSS))
                .fontFaceCss(renderTarget == DocumentRenderTarget.PDF_PRINT
                        ? buildFontFaceCss(fontRenderProfile)
                        : "")
                .primaryColor("#2B5E74")
                .primaryDarkColor("#173847")
                .textColor("#1F2D36")
                .labelColor("#51606B")
                .mutedColor("#7C8A96")
                .borderColor("#D5DEE6")
                .headerBorderColor("#BCC8D1")
                .stripeColor("#F7FAFC")
                .headerColor("#EDF3F7")
                .softColor("#F5F8FA")
                .coverTitleSize("24pt")
                .sectionTitleSize("15pt")
                .subTitleSize("11pt")
                .bodyFontSize("10pt")
                .lineHeight("1.65")
                .pageMargin("12mm 11mm")
                .build();
    }

    private static String resolveFontCss(FontRenderProfile fontRenderProfile,
                                         DocumentRenderTarget renderTarget,
                                         java.util.function.Function<FontRenderProfile, String> pdfAccessor,
                                         java.util.function.Function<FontRenderProfile, String> previewAccessor,
                                         String defaultValue) {
        if (renderTarget == DocumentRenderTarget.PDF_PRINT) {
            return StrUtil.blankToDefault(fontRenderProfile == null ? null : pdfAccessor.apply(fontRenderProfile), defaultValue);
        }
        return StrUtil.blankToDefault(fontRenderProfile == null ? null : previewAccessor.apply(fontRenderProfile), defaultValue);
    }

    private static String buildFontFaceCss(FontRenderProfile fontRenderProfile) {
        if (fontRenderProfile == null || cn.hutool.core.collection.CollUtil.isEmpty(fontRenderProfile.getPdfFontResources())) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (PdfFontResource resource : fontRenderProfile.getPdfFontResources()) {
            if (StrUtil.isBlank(resource.getFamily()) || StrUtil.isBlank(resource.getSourceUri())) {
                continue;
            }
            builder.append("@font-face {")
                    .append("font-family: \"").append(resource.getFamily()).append("\";")
                    .append("src: url(\"").append(resource.getSourceUri()).append("\");")
                    .append("-fs-pdf-font-embed: embed;")
                    .append("-fs-pdf-font-encoding: Identity-H;")
                    .append("}\n");
        }
        return builder.toString();
    }
}
