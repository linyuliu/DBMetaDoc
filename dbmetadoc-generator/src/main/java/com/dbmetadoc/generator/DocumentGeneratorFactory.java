package com.dbmetadoc.generator;

public class DocumentGeneratorFactory {

    public static DocumentGenerator create(String format) {
        if (format == null) {
            throw new IllegalArgumentException("Format must not be null");
        }
        switch (format.toUpperCase()) {
            case "HTML":
                return new HtmlDocumentGenerator();
            case "MARKDOWN":
            case "MD":
                return new MarkdownDocumentGenerator();
            case "PDF":
                return new PdfDocumentGenerator();
            case "WORD":
            case "DOC":
            case "DOCX":
                return new WordDocumentGenerator();
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
}
