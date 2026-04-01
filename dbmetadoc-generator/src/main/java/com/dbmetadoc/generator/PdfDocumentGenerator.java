package com.dbmetadoc.generator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lowagie.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * PDF 文档生成器。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Slf4j
public class PdfDocumentGenerator implements DocumentGenerator {

    private final HtmlDocumentGenerator htmlGenerator = new HtmlDocumentGenerator();

    @Override
    public String getFormat() {
        return "PDF";
    }

    @Override
    public byte[] generate(DocumentRenderContext renderContext) throws Exception {
        String html = htmlGenerator.generateHtml(renderContext, DocumentRenderTarget.PDF_PRINT);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.getSharedContext().setPrint(true);
            renderer.getSharedContext().setInteractive(false);
            registerFonts(renderer, renderContext);
            renderer.setDocumentFromString(html, new File(".").toURI().toString());
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void registerFonts(ITextRenderer renderer, DocumentRenderContext renderContext) {
        if (renderContext.getFontProfile() == null || CollUtil.isEmpty(renderContext.getFontProfile().getPdfFontFiles())) {
            log.warn("PDF 生成未发现可注册字体文件，将尝试使用默认字体，中文可能无法正常渲染。"
                    + "如需显式指定字体目录，请设置环境变量 DBMETADOC_FONT_DIRECTORIES");
            return;
        }
        if (CollUtil.isEmpty(renderContext.getFontProfile().getPdfFontResources())) {
            log.warn("PDF 生成未发现可嵌入的 TTF/OTF 中文字体，将仅依赖字体注册回退。"
                    + "如需修复中文丢失，请通过 DBMETADOC_FONT_DIRECTORIES 指向包含 TTF/OTF 中文字体的目录");
        } else {
            log.info("PDF 主题字体别名：title={}，body={}，mono={}，嵌入资源数={}",
                    StrUtil.blankToDefault(renderContext.getFontProfile().getPdfTitleFontCss(), "(none)"),
                    StrUtil.blankToDefault(renderContext.getFontProfile().getPdfBodyFontCss(), "(none)"),
                    StrUtil.blankToDefault(renderContext.getFontProfile().getPdfMonoFontCss(), "(none)"),
                    renderContext.getFontProfile().getPdfFontResources().size());
        }
        int aliasLoadedCount = 0;
        Set<String> aliasRegisteredPaths = new LinkedHashSet<>();
        for (PdfFontResource resource : renderContext.getFontProfile().getPdfFontResources()) {
            try {
                String resolvedPath = resolveFontPath(resource.getSourceUri());
                if (StrUtil.isBlank(resource.getFamily()) || StrUtil.isBlank(resolvedPath)) {
                    continue;
                }
                renderer.getFontResolver().addFont(
                        resolvedPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, resource.getFamily());
                aliasRegisteredPaths.add(resolvedPath);
                aliasLoadedCount++;
            } catch (Exception ex) {
                log.debug("跳过无法按别名注册的 PDF 字体资源：family={}，source={}，原因：{}",
                        resource.getFamily(), resource.getSourceUri(), ex.getMessage());
            }
        }
        int loadedCount = 0;
        for (String fontFile : renderContext.getFontProfile().getPdfFontFiles()) {
            try {
                if (StrUtil.isBlank(fontFile)) {
                    continue;
                }
                if (aliasRegisteredPaths.contains(fontFile)) {
                    continue;
                }
                if (fontFile.toLowerCase(Locale.ROOT).endsWith(".ttc")) {
                    // TTC (TrueType Collection) 需要按索引逐个注册
                    for (int i = 0; i < 10; i++) {
                        try {
                            renderer.getFontResolver().addFont(
                                    fontFile + "," + i, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                            loadedCount++;
                        } catch (Exception ignored) {
                            break;
                        }
                    }
                } else {
                    renderer.getFontResolver().addFont(fontFile, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    loadedCount++;
                }
            } catch (Exception ex) {
                log.debug("跳过无法加载的 PDF 字体文件：{}，原因：{}", fontFile, ex.getMessage());
            }
        }
        log.info("PDF 字体注册完成，别名注册 {} 个，候选字体文件 {} 个，实际回退注册 {} 个",
                aliasLoadedCount, renderContext.getFontProfile().getPdfFontFiles().size(), loadedCount);
    }

    private String resolveFontPath(String sourceUri) {
        if (StrUtil.isBlank(sourceUri)) {
            return null;
        }
        if (sourceUri.startsWith("file:")) {
            return Path.of(URI.create(sourceUri)).toString();
        }
        return sourceUri;
    }
}
