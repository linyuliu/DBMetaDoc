package com.dbmetadoc.generator;

import com.dbmetadoc.common.model.DatabaseInfo;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

public class PdfDocumentGenerator implements DocumentGenerator {

    private final HtmlDocumentGenerator htmlGenerator = new HtmlDocumentGenerator();

    @Override
    public String getFormat() {
        return "PDF";
    }

    @Override
    public byte[] generate(DatabaseInfo databaseInfo, String title) throws Exception {
        String html = htmlGenerator.generateHtml(databaseInfo, title);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }
}
