package com.dbmetadoc.generator;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Markdown 文档生成器。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class MarkdownDocumentGenerator implements DocumentGenerator {

    @Override
    public String getFormat() {
        return "MARKDOWN";
    }

    @Override
    public byte[] generate(DocumentRenderContext renderContext) throws Exception {
        Map<String, Object> model = FreeMarkerConfig.buildModel(renderContext);
        String markdown = FreeMarkerConfig.render("markdown/database.ftl", model);
        return markdown.getBytes(StandardCharsets.UTF_8);
    }
}
