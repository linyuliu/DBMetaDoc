package com.dbmetadoc.generator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.generator.model.DocumentColumnModel;
import com.dbmetadoc.generator.model.DocumentForeignKeyModel;
import com.dbmetadoc.generator.model.DocumentIndexModel;
import com.dbmetadoc.generator.model.DocumentTableModel;
import com.dbmetadoc.generator.model.DocumentTemplateModel;
import com.dbmetadoc.generator.support.GeneratorSupport;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;

/**
 * Word 文档生成器。
 *
 * @author mumu
 * @date 2026-03-31
 */
public class WordDocumentGenerator implements DocumentGenerator {

    private static final long A4_WIDTH_TWIPS = 11906L;
    private static final long A4_HEIGHT_TWIPS = 16838L;
    private static final int PAGE_MARGIN_TWIPS = 720;
    private static final int PAGE_TOP_BOTTOM_TWIPS = 900;
    private static final String COLOR_TITLE = "1F2F45";
    private static final String COLOR_SUBTITLE = "607287";
    private static final String COLOR_BORDER = "D7DDEA";
    private static final String COLOR_HEADER = "EEF3F9";
    private static final String COLOR_HEADER_DIRECTORY = "F4F6FA";
    private static final String COLOR_DIR_LINE = "C5D1E0";
    private static final String COLOR_STRIPE = "FAFBFD";
    private static final int WIDTH_DIR_NO = 900;
    private static final int WIDTH_DIR_NAME = 6200;
    private static final int WIDTH_DIR_COMMENT = 5200;
    private static final int WIDTH_COL_NO = 620;
    private static final int WIDTH_COL_NAME = 2280;
    private static final int WIDTH_COL_TYPE = 1320;
    private static final int WIDTH_COL_LENGTH = 820;
    private static final int WIDTH_COL_SCALE = 900;
    private static final int WIDTH_COL_FLAG = 620;
    private static final int WIDTH_COL_NULLABLE = 760;
    private static final int WIDTH_COL_DEFAULT = 1120;
    private static final int WIDTH_COL_COMMENT = 5450;
    private static final int WIDTH_INDEX_NAME = 3400;
    private static final int WIDTH_INDEX_COLUMNS = 5400;
    private static final int WIDTH_INDEX_FLAG = 1200;
    private static final int WIDTH_INDEX_TYPE = 1800;
    private static final int WIDTH_FK_NAME = 3200;
    private static final int WIDTH_FK_COLUMN = 2200;
    private static final int WIDTH_FK_REF_TABLE = 5200;
    private static final int WIDTH_FK_REF_COLUMN = 2200;
    private static final int WIDTH_EXT_NO = 800;
    private static final int WIDTH_EXT_NAME = 2200;
    private static final int WIDTH_EXT_RAW = 2600;
    private static final int WIDTH_EXT_JAVA = 2000;
    private static final int WIDTH_EXT_SUMMARY = 5200;

    @Override
    public String getFormat() {
        return "WORD";
    }

    @Override
    public byte[] generate(DocumentRenderContext renderContext) throws Exception {
        DocumentTemplateModel view = DocumentTemplateModelFactory.create(renderContext);
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            FontPalette fonts = FontPalette.from(renderContext.getFontProfile());
            applyPageLayout(document);
            writeCover(document, view, fonts);
            if (GeneratorSupport.hasItems(view.getTables())) {
                writeDirectory(document, view, fonts);
                for (int index = 0; index < view.getTables().size(); index++) {
                    writeTableSection(document, view.getTables().get(index), fonts, index > 0);
                }
            } else {
                addPageBreak(document);
                addParagraph(document, "当前未查询到可导出的表结构。", ParagraphAlignment.CENTER, fonts.body, 11, false, COLOR_SUBTITLE, 0, 120);
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "Word 文档生成失败: " + ex.getMessage(), ex);
        }
    }

    private void applyPageLayout(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSz.setW(BigInteger.valueOf(A4_HEIGHT_TWIPS));
        pageSz.setH(BigInteger.valueOf(A4_WIDTH_TWIPS));
        pageSz.setOrient(STPageOrientation.LANDSCAPE);

        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(PAGE_TOP_BOTTOM_TWIPS));
        pageMar.setBottom(BigInteger.valueOf(PAGE_TOP_BOTTOM_TWIPS));
        pageMar.setLeft(BigInteger.valueOf(PAGE_MARGIN_TWIPS));
        pageMar.setRight(BigInteger.valueOf(PAGE_MARGIN_TWIPS));
        pageMar.setHeader(BigInteger.valueOf(420));
        pageMar.setFooter(BigInteger.valueOf(420));
    }

    private void writeCover(XWPFDocument document, DocumentTemplateModel view, FontPalette fonts) {
        addParagraph(document, "数据库结构文档", ParagraphAlignment.CENTER, fonts.mono, 10, false, COLOR_SUBTITLE, 360, 80);
        addParagraph(document, view.getTitle(), ParagraphAlignment.CENTER, fonts.title, 20, true, COLOR_TITLE, 0, 90);
        addParagraph(document, view.getDatabaseName() + " · 共 " + view.getTableCount() + " 张表",
                ParagraphAlignment.CENTER, fonts.body, 11, false, COLOR_SUBTITLE, 0, 40);
        addParagraph(document, "生成时间：" + view.getGeneratedAt(),
                ParagraphAlignment.CENTER, fonts.body, 10, false, COLOR_SUBTITLE, 0, 120);
    }

    private void writeDirectory(XWPFDocument document, DocumentTemplateModel view, FontPalette fonts) {
        addPageBreak(document);
        addParagraph(document, "数 据 表 目 录", ParagraphAlignment.CENTER, fonts.title, 22, true, COLOR_TITLE, 120, 10);
        addParagraph(document, view.getDatabaseName(), ParagraphAlignment.CENTER, fonts.title, 13, true, COLOR_SUBTITLE, 0, 8);
        addParagraph(document, "共 " + view.getTableCount() + " 张表", ParagraphAlignment.CENTER, fonts.body, 10, false, COLOR_SUBTITLE, 0, 36);
        addDivider(document, COLOR_DIR_LINE, 16, 100);
        XWPFTable table = createTable(document);
        setTableColumnWidths(table, WIDTH_DIR_NO, WIDTH_DIR_NAME, WIDTH_DIR_COMMENT);
        writeHeaderRow(table, List.of("序号", "表名", "表说明"), fonts, List.of(
                ParagraphAlignment.CENTER,
                ParagraphAlignment.CENTER,
                ParagraphAlignment.LEFT
        ), COLOR_HEADER_DIRECTORY, 9);
        int rowIndex = 1;
        for (DocumentTableModel tableModel : view.getTables()) {
            XWPFTableRow row = table.createRow();
            styleRow(row);
            writeCell(row.getCell(0), String.valueOf(tableModel.getTableNo()), fonts.body, 10, true, ParagraphAlignment.CENTER, false);
            writeCell(row.getCell(1), fullTableName(tableModel), fonts.mono, 10, true, ParagraphAlignment.CENTER, false);
            writeCell(row.getCell(2), GeneratorSupport.safeText(tableModel.getComment()), fonts.body, 10, false, ParagraphAlignment.LEFT, false);
            applyStripe(row, rowIndex++);
        }
    }

    private void writeTableSection(XWPFDocument document, DocumentTableModel table, FontPalette fonts, boolean pageBreakBefore) {
        if (pageBreakBefore) {
            addPageBreak(document);
        }
        addParagraph(document, table.getTableNo() + ". " + fullTableName(table),
                ParagraphAlignment.CENTER, fonts.title, 15, true, COLOR_TITLE, 0, 34);
        addParagraph(document, GeneratorSupport.defaultText(table.getComment(), "未填写表说明"),
                ParagraphAlignment.LEFT, fonts.body, 10, false, COLOR_SUBTITLE, 0, 36);
        addDivider(document, COLOR_DIR_LINE, 0, 56);

        if (Boolean.TRUE.equals(table.getHasBasicColumns())) {
            XWPFTable columnTable = createTable(document);
            setTableColumnWidths(columnTable,
                    WIDTH_COL_NO,
                    WIDTH_COL_NAME,
                    WIDTH_COL_TYPE,
                    WIDTH_COL_LENGTH,
                    WIDTH_COL_SCALE,
                    WIDTH_COL_FLAG,
                    WIDTH_COL_FLAG,
                    WIDTH_COL_NULLABLE,
                    WIDTH_COL_DEFAULT,
                    WIDTH_COL_COMMENT);
            writeHeaderRow(columnTable, List.of("序号", "列名", "数据类型", "长度", "小数位数", "主键", "自增", "允许空", "默认值", "列说明"),
                    fonts, List.of(
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.CENTER,
                            ParagraphAlignment.LEFT,
                            ParagraphAlignment.LEFT
                    ), COLOR_HEADER, 8);
            int rowIndex = 1;
            for (DocumentColumnModel column : table.getColumns()) {
                XWPFTableRow row = columnTable.createRow();
                styleRow(row);
                writeCell(row.getCell(0), String.valueOf(column.getOrderNo()), fonts.body, 8, true, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(1), GeneratorSupport.safeText(column.getName()), fonts.mono, 8, true, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(2), GeneratorSupport.safeText(column.getType()), fonts.body, 8, true, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(3), GeneratorSupport.safeText(column.getLengthText()), fonts.body, 8, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(4), buildScaleText(column), fonts.body, 8, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(5), GeneratorSupport.safeText(column.getPrimaryKeyText()), fonts.body, 8, true, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(6), GeneratorSupport.safeText(column.getAutoIncrementText()), fonts.body, 8, true, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(7), GeneratorSupport.safeText(column.getNullableText()), fonts.body, 8, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(8), GeneratorSupport.safeText(column.getDefaultValue()), fonts.body, 8, false, ParagraphAlignment.LEFT, false);
                writeCell(row.getCell(9), GeneratorSupport.safeText(column.getComment()), fonts.body, 8, false, ParagraphAlignment.LEFT, false);
                applyStripe(row, rowIndex++);
            }
        }

        if (Boolean.TRUE.equals(table.getHasExtendedColumns())) {
            addParagraph(document, "字段扩展补充", ParagraphAlignment.LEFT, fonts.title, 11, true, COLOR_TITLE, 110, 34);
            XWPFTable extTable = createTable(document);
            setTableColumnWidths(extTable, WIDTH_EXT_NO, WIDTH_EXT_NAME, WIDTH_EXT_RAW, WIDTH_EXT_JAVA, WIDTH_EXT_SUMMARY);
            writeHeaderRow(extTable, List.of("序号", "字段名", "原始类型", "Java 类型", "扩展说明"), fonts,
                    List.of(ParagraphAlignment.CENTER, ParagraphAlignment.CENTER, ParagraphAlignment.CENTER, ParagraphAlignment.CENTER, ParagraphAlignment.LEFT),
                    COLOR_HEADER, 8);
            int rowIndex = 1;
            for (DocumentColumnModel column : table.getExtendedColumns()) {
                XWPFTableRow row = extTable.createRow();
                styleRow(row);
                writeCell(row.getCell(0), String.valueOf(column.getOrderNo()), fonts.body, 9, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(1), GeneratorSupport.safeText(column.getName()), fonts.mono, 9, true, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(2), GeneratorSupport.safeText(column.getRawType()), fonts.body, 9, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(3), GeneratorSupport.safeText(column.getJavaType()), fonts.body, 9, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(4), GeneratorSupport.safeText(column.getExtendedSummary()), fonts.body, 9, false, ParagraphAlignment.LEFT, false);
                applyStripe(row, rowIndex++);
            }
        }

        if (Boolean.TRUE.equals(table.getHasIndexes())) {
            addParagraph(document, "索引信息", ParagraphAlignment.LEFT, fonts.title, 11, true, COLOR_TITLE, 110, 34);
            XWPFTable indexTable = createTable(document);
            setTableColumnWidths(indexTable, WIDTH_INDEX_NAME, WIDTH_INDEX_COLUMNS, WIDTH_INDEX_FLAG, WIDTH_INDEX_TYPE);
            writeHeaderRow(indexTable, List.of("索引名", "包含字段", "唯一", "类型"), fonts,
                    List.of(ParagraphAlignment.CENTER, ParagraphAlignment.CENTER, ParagraphAlignment.CENTER, ParagraphAlignment.CENTER),
                    COLOR_HEADER, 8);
            int rowIndex = 1;
            for (DocumentIndexModel index : table.getIndexes()) {
                XWPFTableRow row = indexTable.createRow();
                styleRow(row);
                writeCell(row.getCell(0), GeneratorSupport.safeText(index.getName()), fonts.mono, 9, true, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(1), GeneratorSupport.safeText(index.getColumnNamesText()), fonts.body, 9, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(2), GeneratorSupport.safeText(index.getUniqueText()), fonts.body, 9, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(3), GeneratorSupport.safeText(index.getType()), fonts.body, 9, false, ParagraphAlignment.CENTER, false);
                applyStripe(row, rowIndex++);
            }
        }

        if (Boolean.TRUE.equals(table.getHasForeignKeys())) {
            addParagraph(document, "外键信息", ParagraphAlignment.LEFT, fonts.title, 11, true, COLOR_TITLE, 110, 34);
            XWPFTable fkTable = createTable(document);
            setTableColumnWidths(fkTable, WIDTH_FK_NAME, WIDTH_FK_COLUMN, WIDTH_FK_REF_TABLE, WIDTH_FK_REF_COLUMN);
            writeHeaderRow(fkTable, List.of("外键名", "本表字段", "引用表", "引用字段"), fonts,
                    List.of(ParagraphAlignment.CENTER, ParagraphAlignment.CENTER, ParagraphAlignment.CENTER, ParagraphAlignment.CENTER),
                    COLOR_HEADER, 8);
            int rowIndex = 1;
            for (DocumentForeignKeyModel foreignKey : table.getForeignKeys()) {
                XWPFTableRow row = fkTable.createRow();
                styleRow(row);
                writeCell(row.getCell(0), GeneratorSupport.safeText(foreignKey.getName()), fonts.mono, 9, true, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(1), GeneratorSupport.safeText(foreignKey.getColumnName()), fonts.mono, 9, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(2), GeneratorSupport.safeText(foreignKey.getReferencedTable()), fonts.mono, 9, false, ParagraphAlignment.CENTER, false);
                writeCell(row.getCell(3), GeneratorSupport.safeText(foreignKey.getReferencedColumn()), fonts.mono, 9, false, ParagraphAlignment.CENTER, false);
                applyStripe(row, rowIndex++);
            }
        }
    }

    private XWPFTable createTable(XWPFDocument document) {
        XWPFTable table = document.createTable(1, 1);
        table.setTableAlignment(TableRowAlign.CENTER);
        table.setWidth("100%");
        table.setCellMargins(40, 70, 40, 70);
        table.removeRow(0);
        CTTblPr tableProperties = table.getCTTbl().getTblPr() == null
                ? table.getCTTbl().addNewTblPr()
                : table.getCTTbl().getTblPr();
        CTTblLayoutType layoutType = tableProperties.isSetTblLayout()
                ? tableProperties.getTblLayout()
                : tableProperties.addNewTblLayout();
        layoutType.setType(STTblLayoutType.FIXED);
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
        return table;
    }

    private void writeHeaderRow(XWPFTable table, List<String> headers, FontPalette fonts, List<ParagraphAlignment> alignments,
                                String headerColor, int fontSize) {
        XWPFTableRow headerRow = table.createRow();
        headerRow.setRepeatHeader(true);
        styleRow(headerRow);
        ensureCellCount(headerRow, headers.size());
        for (int index = 0; index < headers.size(); index++) {
            writeCell(headerRow.getCell(index), headers.get(index), fonts.title, fontSize, true, alignments.get(index), headerColor);
        }
    }

    private void setTableColumnWidths(XWPFTable table, int... widths) {
        if (table.getRows().isEmpty()) {
            return;
        }
        XWPFTableRow row = table.getRow(0);
        ensureCellCount(row, widths.length);
        for (int index = 0; index < widths.length; index++) {
            setCellWidth(row.getCell(index), widths[index]);
        }
    }

    private void styleRow(XWPFTableRow row) {
        row.setHeight(320);
        for (XWPFTableCell cell : row.getTableCells()) {
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        }
    }

    private void ensureCellCount(XWPFTableRow row, int expectedSize) {
        while (row.getTableCells().size() < expectedSize) {
            row.addNewTableCell();
        }
    }

    private void setCellWidth(XWPFTableCell cell, int width) {
        CTTcPr cellProperties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth cellWidth = cellProperties.isSetTcW() ? cellProperties.getTcW() : cellProperties.addNewTcW();
        cellWidth.setType(STTblWidth.DXA);
        cellWidth.setW(BigInteger.valueOf(width));
    }

    private void writeCell(XWPFTableCell cell, String text, String fontFamily, int fontSize, boolean bold,
                           ParagraphAlignment alignment, boolean header) {
        writeCell(cell, text, fontFamily, fontSize, bold, alignment, header ? COLOR_HEADER : "FFFFFF");
    }

    private void writeCell(XWPFTableCell cell, String text, String fontFamily, int fontSize, boolean bold,
                           ParagraphAlignment alignment, String backgroundColor) {
        while (cell.getParagraphs().size() > 0) {
            cell.removeParagraph(0);
        }
        cell.setColor(backgroundColor);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(alignment);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);
        XWPFRun run = paragraph.createRun();
        configureRun(run, ObjectUtil.defaultIfNull(text, ""), fontFamily, fontSize, bold,
                COLOR_HEADER.equals(backgroundColor) || COLOR_HEADER_DIRECTORY.equals(backgroundColor) ? COLOR_TITLE : "000000");
    }

    private void applyStripe(XWPFTableRow row, int rowIndex) {
        if (rowIndex % 2 != 0) {
            return;
        }
        for (XWPFTableCell cell : row.getTableCells()) {
            cell.setColor(COLOR_STRIPE);
        }
    }

    private void addPageBreak(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setPageBreak(true);
    }

    private void addParagraph(XWPFDocument document, String text, ParagraphAlignment alignment, String fontFamily,
                              int fontSize, boolean bold, String color, int spacingBefore, int spacingAfter) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        XWPFRun run = paragraph.createRun();
        configureRun(run, text, fontFamily, fontSize, bold, color);
    }

    private void addDivider(XWPFDocument document, String color, int spacingBefore, int spacingAfter) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        CTPPr paragraphProperties = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTPBdr borders = paragraphProperties.isSetPBdr() ? paragraphProperties.getPBdr() : paragraphProperties.addNewPBdr();
        CTBorder bottom = borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom();
        bottom.setVal(STBorder.SINGLE);
        bottom.setSz(BigInteger.valueOf(8));
        bottom.setSpace(BigInteger.ZERO);
        bottom.setColor(color);
    }

    private void configureRun(XWPFRun run, String text, String fontFamily, int fontSize, boolean bold, String color) {
        run.setText(StrUtil.blankToDefault(text, ""));
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setColor(color);
        run.setUnderline(UnderlinePatterns.NONE);
        run.setFontFamily(fontFamily);
        CTRPr runProperties = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTFonts fonts = runProperties.sizeOfRFontsArray() > 0 ? runProperties.getRFontsArray(0) : runProperties.addNewRFonts();
        fonts.setAscii(fontFamily);
        fonts.setHAnsi(fontFamily);
        fonts.setEastAsia(fontFamily);
        fonts.setCs(fontFamily);
    }

    private String fullTableName(DocumentTableModel table) {
        if (StrUtil.isBlank(table.getSchema())) {
            return GeneratorSupport.safeText(table.getName());
        }
        return table.getSchema() + "." + GeneratorSupport.safeText(table.getName());
    }

    private String buildScaleText(DocumentColumnModel column) {
        String precisionScale = GeneratorSupport.safeText(column.getPrecisionScaleText());
        if (StrUtil.isBlank(precisionScale)) {
            return "";
        }
        return precisionScale.replace("精度：", "").replace(" / 小数位：", "/");
    }

    /**
     * Word 字体方案。
     *
     * @author mumu
     * @date 2026-03-31
     */
    private record FontPalette(String title, String body, String mono) {

        private static final String DEFAULT_TITLE = "Microsoft YaHei";
        private static final String DEFAULT_BODY = "DengXian";
        private static final String DEFAULT_MONO = "Cascadia Mono";

        private static FontPalette from(FontRenderProfile fontProfile) {
            if (fontProfile == null) {
                return new FontPalette(DEFAULT_TITLE, DEFAULT_BODY, DEFAULT_MONO);
            }
            return new FontPalette(
                    StrUtil.blankToDefault(fontProfile.getTitleFont(), DEFAULT_TITLE),
                    StrUtil.blankToDefault(fontProfile.getBodyFont(), DEFAULT_BODY),
                    StrUtil.blankToDefault(fontProfile.getMonoFont(), DEFAULT_MONO)
            );
        }
    }
}
