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

    private static final String DEFAULT_TITLE_FONT = "Microsoft YaHei";
    private static final String DEFAULT_BODY_FONT = "DengXian";
    private static final String DEFAULT_MONO_FONT = "Cascadia Mono";
    private static final String DEFAULT_TITLE_FONT_CSS = "\"Microsoft YaHei\", \"微软雅黑\", sans-serif";
    private static final String DEFAULT_BODY_FONT_CSS = "\"DengXian\", \"等线\", sans-serif";
    private static final String DEFAULT_MONO_FONT_CSS = "\"Cascadia Mono\", \"Consolas\", monospace";

    private DocumentThemeFactory() {
    }

    public static DocumentTheme create(FontRenderProfile fontRenderProfile) {
        return DocumentTheme.builder()
                .titleFont(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getTitleFont(), DEFAULT_TITLE_FONT))
                .bodyFont(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getBodyFont(), DEFAULT_BODY_FONT))
                .monoFont(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getMonoFont(), DEFAULT_MONO_FONT))
                .titleFontCss(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getTitleFontCss(), DEFAULT_TITLE_FONT_CSS))
                .bodyFontCss(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getBodyFontCss(), DEFAULT_BODY_FONT_CSS))
                .monoFontCss(StrUtil.blankToDefault(fontRenderProfile == null ? null : fontRenderProfile.getMonoFontCss(), DEFAULT_MONO_FONT_CSS))
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
}
