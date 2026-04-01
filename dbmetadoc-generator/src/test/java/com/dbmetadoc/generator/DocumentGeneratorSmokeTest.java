package com.dbmetadoc.generator;

import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;
import com.dbmetadoc.generator.support.GeneratorSupport;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文档生成器冒烟测试。
 *
 * @author mumu
 * @date 2026-03-28
 */
class DocumentGeneratorSmokeTest {

    private final DataFormatter dataFormatter = new DataFormatter();

    @Test
    void shouldGenerateAllFormatsWithCoreColumnsAndExtendedSection() throws Exception {
        DocumentRenderContext renderContext = buildRenderContext();

        String html = new String(DocumentGeneratorFactory.create("HTML").generate(renderContext), StandardCharsets.UTF_8);
        assertTrue(html.contains("字段清单"));
        assertTrue(html.contains("字段扩展补充"));
        assertTrue(html.contains("数据库结构文档"));
        assertTrue(html.contains("chapter-number"));
        assertTrue(html.contains(">可空<"));
        assertTrue(html.contains("√"));
        assertTrue(html.contains("×"));
        assertTrue(html.contains("<colgroup>"));
        assertFalse(html.contains(">字段列表<"));
        assertFalse(html.contains("预览目录"));
        assertFalse(html.contains("返回目录"));
        assertFalse(html.contains("表目录"));

        String markdown = new String(DocumentGeneratorFactory.create("MARKDOWN").generate(renderContext), StandardCharsets.UTF_8);
        assertTrue(markdown.contains("字段清单"));
        assertTrue(markdown.contains("字段扩展补充"));
        assertTrue(markdown.contains("| 字段名 | 类型 | 主键 | 可空 | 默认值 | 注释 |"));
        assertTrue(markdown.contains("√"));
        assertTrue(markdown.contains("×"));
        assertFalse(markdown.contains("| 序号 | 字段名 | 类型 | 主键 | 可空 | 默认值 | 注释 |"));

        byte[] pdf = DocumentGeneratorFactory.create("PDF").generate(renderContext);
        assertTrue(pdf.length > 4);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        byte[] word = DocumentGeneratorFactory.create("WORD").generate(renderContext);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(word))) {
            String xml = document.getDocument().xmlText();
            assertTrue(xml.contains("共 1 张表"));
            assertTrue(xml.contains("biz.order_main"));
            assertTrue(xml.contains("序号"));
            assertTrue(xml.contains("列名"));
            assertTrue(xml.contains("可空"));
            assertTrue(xml.contains("√"));
            assertTrue(xml.contains("×"));
            assertTrue(xml.contains("字段扩展补充"));
            assertTrue(xml.contains("数据库结构文档"));
            assertFalse(xml.contains("{{"));
            assertFalse(xml.contains("[field]"));
            assertFalse(xml.contains("字段列表"));
            assertFalse(xml.contains("数 据 表 目 录"));
            assertTrue(hasChapterTitleRuns(document, "1. biz.order_main"));
            assertTrue(hasBoldCell(document, "id"));
        }

        byte[] excel = DocumentGeneratorFactory.create("EXCEL").generate(renderContext);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertEquals("库概览", workbook.getSheetName(0));
            assertTrue(workbook.getSheetName(1).startsWith("01_"));
            assertTrue(sheetContains(workbook, "字段清单"));
            assertTrue(sheetContains(workbook, "字段扩展补充"));
            assertTrue(sheetContains(workbook, "√"));
            assertTrue(sheetContains(workbook, "×"));
            assertFalse(sheetContains(workbook, "表目录"));
        }
    }

    @Test
    void shouldRenderTextBooleanDisplayWhenRequested() throws Exception {
        DocumentRenderContext renderContext = buildRenderContext(GeneratorSupport.BOOLEAN_STYLE_TEXT);

        String html = new String(DocumentGeneratorFactory.create("HTML").generate(renderContext), StandardCharsets.UTF_8);
        String markdown = new String(DocumentGeneratorFactory.create("MARKDOWN").generate(renderContext), StandardCharsets.UTF_8);

        assertTrue(html.contains(">是<"));
        assertTrue(html.contains(">否<"));
        assertFalse(html.contains("√"));
        assertTrue(markdown.contains("| id | BIGINT | 是 | 否 | 0 | 主键 |"));
        assertFalse(markdown.contains("√"));
    }

    private boolean sheetContains(XSSFWorkbook workbook, String expectedText) {
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            var sheet = workbook.getSheetAt(sheetIndex);
            for (var row : sheet) {
                for (var cell : row) {
                    if (expectedText.equals(dataFormatter.formatCellValue(cell))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasChapterTitleRuns(XWPFDocument document, String expectedText) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (expectedText.equals(paragraph.getText()) && paragraph.getRuns().size() >= 3) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBoldCell(XWPFDocument document, String expectedText) {
        for (XWPFTable table : document.getTables()) {
            for (var row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    if (!expectedText.equals(cell.getText())) {
                        continue;
                    }
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getRuns().stream().anyMatch(run -> run.isBold())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private DocumentRenderContext buildRenderContext() {
        return buildRenderContext(null);
    }

    private DocumentRenderContext buildRenderContext(String booleanDisplayStyle) {
        ColumnInfo idColumn = ColumnInfo.builder()
                .name("id")
                .type("BIGINT")
                .rawType("bigint")
                .javaType("Long")
                .primaryKey(true)
                .nullable(false)
                .defaultValue("0")
                .comment("主键")
                .autoIncrement(true)
                .generated(false)
                .length(20)
                .ordinalPosition(1)
                .build();

        ColumnInfo nameColumn = ColumnInfo.builder()
                .name("order_name")
                .type("VARCHAR")
                .rawType("varchar(128)")
                .javaType("String")
                .primaryKey(false)
                .nullable(false)
                .defaultValue("")
                .comment("订单名称")
                .autoIncrement(false)
                .generated(false)
                .length(128)
                .ordinalPosition(2)
                .build();

        TableInfo tableInfo = TableInfo.builder()
                .name("order_main")
                .comment("订单主表")
                .schema("biz")
                .primaryKey("id")
                .tableType("BASE TABLE")
                .engine("InnoDB")
                .charset("utf8mb4")
                .collation("utf8mb4_general_ci")
                .rowFormat("Dynamic")
                .columns(List.of(idColumn, nameColumn))
                .indexes(List.of(IndexInfo.builder()
                        .name("idx_order_name")
                        .columnNames(List.of("order_name"))
                        .unique(false)
                        .type("BTREE")
                        .build()))
                .foreignKeys(List.of(ForeignKeyInfo.builder()
                        .name("fk_order_user")
                        .columnName("user_id")
                        .referencedTable("sys_user")
                        .referencedColumn("id")
                        .build()))
                .build();

        DatabaseInfo databaseInfo = DatabaseInfo.builder()
                .name("dbmeta")
                .type("MYSQL")
                .version("8.4")
                .databaseName("dbmeta")
                .schemaName("biz")
                .catalogName("dbmeta")
                .charset("utf8mb4")
                .collation("utf8mb4_general_ci")
                .tables(List.of(tableInfo))
                .build();

        Set<String> visibleSections = new LinkedHashSet<>(List.of(
                "DATABASE_OVERVIEW",
                "TABLE_OVERVIEW",
                "COLUMN_BASIC",
                "COLUMN_EXTENDED",
                "INDEXES",
                "FOREIGN_KEYS"
        ));

        FontRenderProfile fontProfile = FontRenderProfile.builder()
                .code("modern-cn")
                .label("现代中文")
                .titleFont("Microsoft YaHei")
                .bodyFont("DengXian")
                .monoFont("Cascadia Mono")
                .titleFontCss("\"Microsoft YaHei\", \"微软雅黑\", sans-serif")
                .bodyFontCss("\"DengXian\", \"等线\", sans-serif")
                .monoFontCss("\"Cascadia Mono\", monospace")
                .pdfFontFiles(List.of())
                .build();

        return DocumentRenderContext.builder()
                .title("订单数据库结构文档")
                .database(databaseInfo)
                .visibleSections(visibleSections)
                .fontProfile(fontProfile)
                .booleanDisplayStyle(booleanDisplayStyle)
                .build();
    }
}
