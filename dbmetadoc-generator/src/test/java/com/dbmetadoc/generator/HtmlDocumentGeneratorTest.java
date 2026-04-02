package com.dbmetadoc.generator;

import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.TableInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlDocumentGeneratorTest {

    private final HtmlDocumentGenerator htmlDocumentGenerator = new HtmlDocumentGenerator();
    private final PdfDocumentGenerator pdfDocumentGenerator = new PdfDocumentGenerator();

    @Test
    void shouldSeparatePreviewFontsFromPdfEmbeddedFonts() throws Exception {
        String fakeFontUri = "file:///tmp/dbmetadoc-cjk-test.otf";
        DocumentRenderContext renderContext = buildRenderContext(FontRenderProfile.builder()
                .code("custom")
                .label("Custom")
                .titleFont("Preview Title")
                .bodyFont("Preview Body")
                .monoFont("Preview Mono")
                .titleFontCss("\"Preview Title\", sans-serif")
                .bodyFontCss("\"Preview Body\", sans-serif")
                .monoFontCss("\"Preview Mono\", monospace")
                .pdfTitleFontCss("\"DBMetaDocPdfTitle\", \"Preview Body\", sans-serif")
                .pdfBodyFontCss("\"DBMetaDocPdfBody\", \"Preview Body\", sans-serif")
                .pdfMonoFontCss("\"DBMetaDocPdfMono\", \"Preview Mono\", monospace")
                .pdfFontResources(List.of(
                        PdfFontResource.builder().family("DBMetaDocPdfTitle").sourceUri(fakeFontUri).build(),
                        PdfFontResource.builder().family("DBMetaDocPdfBody").sourceUri(fakeFontUri).build()))
                .pdfFontFiles(List.of())
                .build());

        String previewHtml = htmlDocumentGenerator.generateHtml(renderContext, DocumentRenderTarget.HTML_PREVIEW);
        String pdfHtml = htmlDocumentGenerator.generateHtml(renderContext, DocumentRenderTarget.PDF_PRINT);

        assertTrue(previewHtml.contains("\"Preview Body\", sans-serif"));
        assertTrue(previewHtml.contains("chapter-number"));
        assertTrue(previewHtml.contains(">可空<"));
        assertTrue(previewHtml.contains("head-short"));
        assertTrue(previewHtml.contains("head-nowrap"));
        assertFalse(previewHtml.contains("@font-face"));
        assertFalse(previewHtml.contains(fakeFontUri));

        assertTrue(pdfHtml.contains("@font-face"));
        assertTrue(pdfHtml.contains(fakeFontUri));
        assertTrue(pdfHtml.contains("DBMetaDocPdfBody"));
        assertTrue(pdfHtml.contains("DBMetaDocPdfTitle"));
    }

    @Test
    void shouldGenerateReadableChinesePdfWhenEmbeddableFontIsAvailable() throws Exception {
        Path fontPath = findEmbeddableChineseFont();
        Assumptions.assumeTrue(fontPath != null, "No embeddable Chinese font available for PDF smoke test");

        FontRenderProfile fontProfile = FontRenderProfile.builder()
                .code("pdf-test")
                .label("PDF Test")
                .titleFont("PDF Test Title")
                .bodyFont("PDF Test Body")
                .monoFont("PDF Test Mono")
                .titleFontCss("\"PDF Test Title\", sans-serif")
                .bodyFontCss("\"PDF Test Body\", sans-serif")
                .monoFontCss("\"PDF Test Mono\", monospace")
                .pdfTitleFontCss("\"DBMetaDocPdfTitle\", sans-serif")
                .pdfBodyFontCss("\"DBMetaDocPdfBody\", sans-serif")
                .pdfMonoFontCss("\"DBMetaDocPdfMono\", sans-serif")
                .pdfFontFiles(List.of(fontPath.toAbsolutePath().toString()))
                .pdfFontResources(List.of(
                        PdfFontResource.builder().family("DBMetaDocPdfTitle").sourceUri(fontPath.toUri().toString()).build(),
                        PdfFontResource.builder().family("DBMetaDocPdfBody").sourceUri(fontPath.toUri().toString()).build(),
                        PdfFontResource.builder().family("DBMetaDocPdfMono").sourceUri(fontPath.toUri().toString()).build()))
                .build();
        DocumentRenderContext renderContext = buildRenderContext(fontProfile);

        byte[] pdf = pdfDocumentGenerator.generate(renderContext);

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("数据库结构文档"));
            assertTrue(text.contains("订单主表"));
            assertTrue(text.contains("订单标题"));
        }
    }

    private DocumentRenderContext buildRenderContext(FontRenderProfile fontProfile) {
        ColumnInfo column = ColumnInfo.builder()
                .name("order_title")
                .type("VARCHAR")
                .rawType("varchar(128)")
                .javaType("String")
                .nullable(false)
                .primaryKey(false)
                .defaultValue("")
                .comment("订单标题")
                .ordinalPosition(1)
                .build();

        TableInfo table = TableInfo.builder()
                .name("order_main")
                .schema("biz")
                .comment("订单主表")
                .columns(List.of(column))
                .indexes(List.of())
                .foreignKeys(List.of())
                .build();

        DatabaseInfo databaseInfo = DatabaseInfo.builder()
                .name("dbmeta")
                .type("MYSQL")
                .databaseName("dbmeta")
                .schemaName("biz")
                .tables(List.of(table))
                .build();

        Set<String> sections = new LinkedHashSet<>(List.of("DATABASE_OVERVIEW", "COLUMN_BASIC"));
        return DocumentRenderContext.builder()
                .title("订单数据库结构文档")
                .database(databaseInfo)
                .visibleSections(sections)
                .fontProfile(fontProfile)
                .build();
    }

    private Path findEmbeddableChineseFont() {
        List<Path> candidates = List.of(
                Paths.get("C:\\Windows\\Fonts\\Deng.ttf"),
                Paths.get("C:\\Windows\\Fonts\\simhei.ttf"),
                Paths.get("C:\\Windows\\Fonts\\simsunb.ttf"),
                Paths.get("C:\\Windows\\Fonts\\SourceHanSansCN-Regular.otf"),
                Paths.get("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
                Paths.get("/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf"),
                Paths.get("/System/Library/Fonts/PingFang.ttc"));
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                String lower = candidate.getFileName().toString().toLowerCase();
                if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
