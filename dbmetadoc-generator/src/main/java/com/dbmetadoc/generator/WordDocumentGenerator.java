package com.dbmetadoc.generator;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.generator.model.DocumentTemplateModel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Word 文档生成器。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class WordDocumentGenerator implements DocumentGenerator {

    private static final String TEMPLATE_PATH = "/templates/word/database-template.docx";

    @Override
    public String getFormat() {
        return "WORD";
    }

    @Override
    public byte[] generate(DocumentRenderContext renderContext) throws Exception {
        DocumentTemplateModel view = DocumentTemplateModelFactory.create(renderContext);
        Configure configure = Configure.builder()
                .bind("tableOverviewRows", new LoopRowTableRenderPolicy())
                .bind("columns", new LoopRowTableRenderPolicy())
                .bind("extendedColumns", new LoopRowTableRenderPolicy())
                .bind("indexes", new LoopRowTableRenderPolicy())
                .bind("foreignKeys", new LoopRowTableRenderPolicy())
                .build();

        InputStream resourceStream = WordDocumentGenerator.class.getResourceAsStream(TEMPLATE_PATH);
        if (resourceStream == null) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "Word 模板不存在: " + TEMPLATE_PATH);
        }

        try (InputStream templateStream = resourceStream;
             XWPFTemplate template = XWPFTemplate.compile(templateStream, configure).render(view);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            template.write(outputStream);
            return outputStream.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "Word 文档生成失败: " + ex.getMessage(), ex);
        }
    }
}
