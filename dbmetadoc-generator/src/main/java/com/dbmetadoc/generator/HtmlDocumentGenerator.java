package com.dbmetadoc.generator;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTML 文档生成器。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class HtmlDocumentGenerator implements DocumentGenerator {

    @Override
    public String getFormat() {
        return "HTML";
    }

    @Override
    public byte[] generate(DocumentRenderContext renderContext) throws Exception {
        String html = generateHtml(renderContext);
        return html.getBytes(StandardCharsets.UTF_8);
    }

    public String generateHtml(DocumentRenderContext renderContext) throws Exception {
        return generateHtml(renderContext, DocumentRenderTarget.HTML_PREVIEW);
    }

    public String generateHtml(DocumentRenderContext renderContext, DocumentRenderTarget renderTarget) throws Exception {
        Map<String, Object> model = FreeMarkerConfig.buildModel(renderContext, renderTarget);
        return FreeMarkerConfig.render("html/database.ftl", model);
    }
}
