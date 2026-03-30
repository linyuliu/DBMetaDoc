package com.dbmetadoc.generator;

import com.dbmetadoc.generator.model.DocumentTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Freemarker 模板配置。
 *
 * @author mumu
 * @date 2026-03-30
 */
public final class FreeMarkerConfig {

    private static volatile Configuration instance;

    private FreeMarkerConfig() {
    }

    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (FreeMarkerConfig.class) {
                if (instance == null) {
                    Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);
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
        try (StringWriter writer = new StringWriter()) {
            template.process(model, writer);
            return writer.toString();
        }
    }

    public static Map<String, Object> buildModel(DocumentRenderContext renderContext) {
        DocumentTemplateModel view = DocumentTemplateModelFactory.create(renderContext);
        Map<String, Object> model = new HashMap<>();
        model.put("view", view);
        model.put("theme", view.getTheme());
        return model;
    }
}
