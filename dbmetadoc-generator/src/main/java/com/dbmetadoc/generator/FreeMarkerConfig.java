package com.dbmetadoc.generator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class FreeMarkerConfig {

    private static volatile Configuration instance;

    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (FreeMarkerConfig.class) {
                if (instance == null) {
                    Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
                    cfg.setClassForTemplateLoading(FreeMarkerConfig.class, "/templates");
                    cfg.setDefaultEncoding("UTF-8");
                    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
                    cfg.setLogTemplateExceptions(false);
                    instance = cfg;
                }
            }
        }
        return instance;
    }

    public static String render(String templatePath, Map<String, Object> model) throws Exception {
        Template template = getInstance().getTemplate(templatePath);
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }

    public static Map<String, Object> buildModel(Object database, String title) {
        Map<String, Object> model = new HashMap<>();
        model.put("database", database);
        model.put("title", title != null ? title : "Database Documentation");
        return model;
    }
}
