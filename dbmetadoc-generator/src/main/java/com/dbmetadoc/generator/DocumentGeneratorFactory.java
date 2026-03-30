package com.dbmetadoc.generator;

import cn.hutool.core.util.StrUtil;


/**
 * 文档生成器工厂。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class DocumentGeneratorFactory {

    public static DocumentGenerator create(String format) {
        if (StrUtil.isBlank(format)) {
            throw new IllegalArgumentException("Format must not be blank");
        }
        return switch (format.toUpperCase()) {
            case "HTML" -> new HtmlDocumentGenerator();
            case "MARKDOWN", "MD" -> new MarkdownDocumentGenerator();
            case "PDF" -> new PdfDocumentGenerator();
            case "WORD", "DOC", "DOCX" -> new WordDocumentGenerator();
            case "EXCEL", "XLS", "XLSX" -> new ExcelDocumentGenerator();
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }
}
