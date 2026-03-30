package com.dbmetadoc.generator;

import com.lowagie.text.pdf.BaseFont;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * PDF 文档生成器。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class PdfDocumentGenerator implements DocumentGenerator {

    private final HtmlDocumentGenerator htmlGenerator = new HtmlDocumentGenerator();

    @Override
    public String getFormat() {
        return "PDF";
    }

    @Override
    public byte[] generate(DocumentRenderContext renderContext) throws Exception {
        String html = htmlGenerator.generateHtml(renderContext);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.getSharedContext().setPrint(true);
            renderer.getSharedContext().setInteractive(false);
            registerFonts(renderer, renderContext);
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void registerFonts(ITextRenderer renderer, DocumentRenderContext renderContext) {
        if (renderContext.getFontProfile() == null || renderContext.getFontProfile().getPdfFontFiles() == null) {
            return;
        }
        for (String fontFile : renderContext.getFontProfile().getPdfFontFiles()) {
            try {
                if (fontFile.toLowerCase(Locale.ROOT).endsWith(".ttc")) {
                    // TTC (TrueType Collection) 需要按索引逐个注册
                    for (int i = 0; i < 10; i++) {
                        try {
                            renderer.getFontResolver().addFont(
                                    fontFile + "," + i, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        } catch (Exception ignored) {
                            break;
                        }
                    }
                } else {
                    renderer.getFontResolver().addFont(fontFile, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {
                // 跳过无法加载的字体文件
            }
        }
    }
}
