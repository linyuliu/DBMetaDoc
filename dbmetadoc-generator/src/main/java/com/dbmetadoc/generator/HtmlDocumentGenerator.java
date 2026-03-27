package com.dbmetadoc.generator;

import com.dbmetadoc.common.model.DatabaseInfo;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HtmlDocumentGenerator implements DocumentGenerator {

    @Override
    public String getFormat() {
        return "HTML";
    }

    @Override
    public byte[] generate(DatabaseInfo databaseInfo, String title) throws Exception {
        String html = generateHtml(databaseInfo, title);
        return html.getBytes(StandardCharsets.UTF_8);
    }

    public String generateHtml(DatabaseInfo databaseInfo, String title) throws Exception {
        Map<String, Object> model = FreeMarkerConfig.buildModel(databaseInfo, title);
        return FreeMarkerConfig.render("html/database.ftl", model);
    }
}
