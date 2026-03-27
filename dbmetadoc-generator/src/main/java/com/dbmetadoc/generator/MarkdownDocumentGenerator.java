package com.dbmetadoc.generator;

import com.dbmetadoc.common.model.DatabaseInfo;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MarkdownDocumentGenerator implements DocumentGenerator {

    @Override
    public String getFormat() {
        return "MARKDOWN";
    }

    @Override
    public byte[] generate(DatabaseInfo databaseInfo, String title) throws Exception {
        Map<String, Object> model = FreeMarkerConfig.buildModel(databaseInfo, title);
        String markdown = FreeMarkerConfig.render("markdown/database.ftl", model);
        return markdown.getBytes(StandardCharsets.UTF_8);
    }
}
