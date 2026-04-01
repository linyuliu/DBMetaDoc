package com.dbmetadoc.generator;

import cn.hutool.core.collection.CollUtil;
import com.dbmetadoc.generator.model.DocumentColumnModel;
import com.dbmetadoc.generator.model.DocumentForeignKeyModel;
import com.dbmetadoc.generator.model.DocumentIndexModel;
import com.dbmetadoc.generator.model.DocumentTableModel;
import com.dbmetadoc.generator.model.DocumentTableLayout;
import com.dbmetadoc.generator.model.DocumentTemplateModel;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Excel 文档生成器。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class ExcelDocumentGenerator implements DocumentGenerator {

    private static final float DEFAULT_ROW_HEIGHT = 21f;
    private static final float TITLE_ROW_HEIGHT = 28f;
    private static final IndexedColors TITLE_BG = IndexedColors.GREY_25_PERCENT;
    private static final IndexedColors SECTION_BG = IndexedColors.PALE_BLUE;
    private static final IndexedColors HEADER_BG = IndexedColors.LIGHT_CORNFLOWER_BLUE;
    private static final IndexedColors LABEL_BG = IndexedColors.LEMON_CHIFFON;

    @Override
    public String getFormat() {
        return "EXCEL";
    }

    @Override
    public byte[] generate(DocumentRenderContext renderContext) throws Exception {
        DocumentTemplateModel view = DocumentTemplateModelFactory.create(renderContext);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook, renderContext.getFontProfile());
            writeOverviewSheet(workbook, view, styles);
            writeTableSheets(workbook, view, styles);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeOverviewSheet(XSSFWorkbook workbook, DocumentTemplateModel view, Styles styles) {
        Sheet sheet = workbook.createSheet("库概览");
        int rowIndex = 0;
        rowIndex = writeMergedTitle(sheet, rowIndex, 0, 5, view.getTitle(), styles.title);
        rowIndex = writeKeyValue(sheet, rowIndex, "数据库名称", view.getDatabaseName(), styles);
        rowIndex = writeKeyValue(sheet, rowIndex, "表数量", String.valueOf(view.getTableCount()), styles);
        rowIndex = writeKeyValue(sheet, rowIndex, "生成时间", view.getGeneratedAt(), styles);
        if (Boolean.TRUE.equals(view.getShowDatabaseOverview())) {
            rowIndex = writeKeyValue(sheet, rowIndex, "数据库类型", view.getType(), styles);
            rowIndex = writeKeyValue(sheet, rowIndex, "Schema", view.getSchemaName(), styles);
            rowIndex = writeKeyValue(sheet, rowIndex, "Catalog", view.getCatalogName(), styles);
            rowIndex = writeKeyValue(sheet, rowIndex, "字符集", view.getCharset(), styles);
            rowIndex = writeKeyValue(sheet, rowIndex, "排序规则", view.getCollation(), styles);
            rowIndex = writeKeyValue(sheet, rowIndex, "版本", view.getVersion(), styles);
        }
        setColumnWidths(sheet, 10, 22, 18, 10, 18, 32);
    }

    private void writeTableSheets(XSSFWorkbook workbook, DocumentTemplateModel view, Styles styles) {
        if (CollUtil.isEmpty(view.getTables())) {
            return;
        }
        for (DocumentTableModel table : view.getTables()) {
            String sheetName = WorkbookUtil.createSafeSheetName(String.format("%02d_%s", table.getTableNo(), table.getName()));
            Sheet sheet = workbook.createSheet(sheetName);
            int rowIndex = 0;
            rowIndex = writeMergedTitle(sheet, rowIndex, 0, 6, table.getChapterTitle(), styles.title);

            rowIndex = writeKeyValue(sheet, rowIndex, "表说明", table.getComment(), styles);

            if (Boolean.TRUE.equals(table.getHasBasicColumns())) {
                rowIndex++;
                rowIndex = writeSectionTitle(sheet, rowIndex, 0, 6, "字段清单", styles.section);
                rowIndex = writeHeaderRow(sheet, rowIndex, List.of("序号", "列名", "数据类型", "主键", "可空", "默认值", "列说明"), styles.header);
                for (int columnIndex = 0; columnIndex < table.getColumns().size(); columnIndex++) {
                    DocumentColumnModel column = table.getColumns().get(columnIndex);
                    rowIndex = writeRow(sheet, rowIndex, List.of(
                            String.valueOf(column.getOrderNo()),
                            column.getName(),
                            column.getType(),
                            column.getPrimaryKeyText(),
                            column.getNullableText(),
                            column.getDefaultValue(),
                            column.getComment()
                    ), rowStyles(styles.bodyStrong, styles.bodyStrong, styles.bodyStrong, styles.bodyStrong, styles.bodyStrong, styles.bodyStrong, styles.bodyStrong),
                            rowHeightForLines(table.getBasicColumnLayout(), columnIndex));
                }
            }

            if (Boolean.TRUE.equals(table.getHasExtendedColumns())) {
                rowIndex++;
                rowIndex = writeSectionTitle(sheet, rowIndex, 0, 4, "字段扩展补充", styles.section);
                rowIndex = writeHeaderRow(sheet, rowIndex, List.of("序号", "字段名", "原始类型", "Java 类型", "扩展说明"), styles.header);
                for (int columnIndex = 0; columnIndex < table.getExtendedColumns().size(); columnIndex++) {
                    DocumentColumnModel column = table.getExtendedColumns().get(columnIndex);
                    rowIndex = writeRow(sheet, rowIndex, List.of(
                            String.valueOf(column.getOrderNo()),
                            column.getName(),
                            column.getRawType(),
                            column.getJavaType(),
                            column.getExtendedSummary()
                    ), rowStyles(styles.bodyStrong, styles.bodyStrong, styles.bodyStrong, styles.bodyStrong, styles.bodyStrong),
                            rowHeightForLines(table.getExtendedColumnLayout(), columnIndex));
                }
            }

            if (Boolean.TRUE.equals(table.getHasIndexes())) {
                rowIndex++;
                rowIndex = writeSectionTitle(sheet, rowIndex, 0, 3, "索引信息", styles.section);
                rowIndex = writeHeaderRow(sheet, rowIndex, List.of("索引名", "包含字段", "唯一", "类型"), styles.header);
                for (int indexNo = 0; indexNo < table.getIndexes().size(); indexNo++) {
                    DocumentIndexModel index = table.getIndexes().get(indexNo);
                    rowIndex = writeRow(sheet, rowIndex, List.of(
                            index.getName(),
                            index.getColumnNamesText(),
                            index.getUniqueText(),
                            index.getType()
                    ), rowStyles(styles.bodyStrong, styles.bodyStrong, styles.bodyStrong, styles.bodyStrong),
                            rowHeightForLines(table.getIndexLayout(), indexNo));
                }
            }

            if (Boolean.TRUE.equals(table.getHasForeignKeys())) {
                rowIndex++;
                rowIndex = writeSectionTitle(sheet, rowIndex, 0, 3, "外键信息", styles.section);
                rowIndex = writeHeaderRow(sheet, rowIndex, List.of("外键名", "本表字段", "引用表", "引用字段"), styles.header);
                for (int foreignKeyIndex = 0; foreignKeyIndex < table.getForeignKeys().size(); foreignKeyIndex++) {
                    DocumentForeignKeyModel foreignKey = table.getForeignKeys().get(foreignKeyIndex);
                    rowIndex = writeRow(sheet, rowIndex, List.of(
                            foreignKey.getName(),
                            foreignKey.getColumnName(),
                            foreignKey.getReferencedTable(),
                            foreignKey.getReferencedColumn()
                    ), rowStyles(styles.bodyStrong, styles.bodyStrong, styles.bodyStrong, styles.bodyStrong),
                            rowHeightForLines(table.getForeignKeyLayout(), foreignKeyIndex));
                }
            }

            applySheetColumnWidths(sheet,
                    table.getBasicColumnLayout(),
                    table.getExtendedColumnLayout(),
                    table.getIndexLayout(),
                    table.getForeignKeyLayout());
            sheet.createFreezePane(0, 1);
        }
    }

    private int writeMergedTitle(Sheet sheet, int rowIndex, int firstCol, int lastCol, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(TITLE_ROW_HEIGHT);
        for (int columnIndex = firstCol; columnIndex <= lastCol; columnIndex++) {
            Cell cell = row.createCell(columnIndex);
            if (columnIndex == firstCol) {
                cell.setCellValue(title);
            }
            cell.setCellStyle(style);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstCol, lastCol));
        return rowIndex + 1;
    }

    private int writeSectionTitle(Sheet sheet, int rowIndex, int firstCol, int lastCol, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(DEFAULT_ROW_HEIGHT);
        for (int columnIndex = firstCol; columnIndex <= lastCol; columnIndex++) {
            Cell cell = row.createCell(columnIndex);
            if (columnIndex == firstCol) {
                cell.setCellValue(title);
            }
            cell.setCellStyle(style);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstCol, lastCol));
        return rowIndex + 1;
    }

    private int writeKeyValue(Sheet sheet, int rowIndex, String key, String value, Styles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(DEFAULT_ROW_HEIGHT);
        createCell(row, 0, key, styles.label);
        createCell(row, 1, value, styles.body);
        return rowIndex + 1;
    }

    private int writeHeaderRow(Sheet sheet, int rowIndex, List<String> headers, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(DEFAULT_ROW_HEIGHT);
        for (int index = 0; index < headers.size(); index++) {
            createCell(row, index, headers.get(index), style);
        }
        return rowIndex + 1;
    }

    private int writeValuesRow(Sheet sheet, int rowIndex, List<String> values, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(DEFAULT_ROW_HEIGHT);
        for (int index = 0; index < values.size(); index++) {
            createCell(row, index, values.get(index), style);
        }
        return rowIndex + 1;
    }

    private int writeRow(Sheet sheet, int rowIndex, List<String> values, List<CellStyle> cellStyles, float rowHeight) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(rowHeight);
        for (int index = 0; index < values.size(); index++) {
            createCell(row, index, values.get(index), cellStyles.get(index));
        }
        return rowIndex + 1;
    }

    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setColumnWidths(Sheet sheet, int... widths) {
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index] * 256);
        }
    }

    private void applySheetColumnWidths(Sheet sheet, DocumentTableLayout... layouts) {
        int maxColumns = 0;
        for (DocumentTableLayout layout : layouts) {
            if (layout != null && layout.getColumns() != null) {
                maxColumns = Math.max(maxColumns, layout.getColumns().size());
            }
        }
        for (int columnIndex = 0; columnIndex < maxColumns; columnIndex++) {
            double maxBudget = 8d;
            for (DocumentTableLayout layout : layouts) {
                if (layout == null || layout.getColumns() == null || columnIndex >= layout.getColumns().size()) {
                    continue;
                }
                maxBudget = Math.max(maxBudget, layout.getColumns().get(columnIndex).getCharacterBudget());
            }
            sheet.setColumnWidth(columnIndex, DocumentTableLayoutCalculator.toExcelWidth(maxBudget) * 256);
        }
    }

    private float rowHeightForLines(DocumentTableLayout layout, int rowIndex) {
        if (layout == null || layout.getRowLineCounts() == null || rowIndex < 0 || rowIndex >= layout.getRowLineCounts().size()) {
            return DEFAULT_ROW_HEIGHT;
        }
        return switch (Math.max(1, Math.min(layout.getRowLineCounts().get(rowIndex), 4))) {
            case 1 -> 21f;
            case 2 -> 33f;
            case 3 -> 45f;
            default -> 57f;
        };
    }

    private List<CellStyle> rowStyles(CellStyle... styles) {
        return List.of(styles);
    }

    private Styles createStyles(XSSFWorkbook workbook, FontRenderProfile fontProfile) {
        Font titleFont = workbook.createFont();
        titleFont.setFontName(fontProfile == null ? "Microsoft YaHei" : fontProfile.getTitleFont());
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font headerFont = workbook.createFont();
        headerFont.setFontName(fontProfile == null ? "DengXian" : fontProfile.getBodyFont());
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 10);

        Font labelFont = workbook.createFont();
        labelFont.setFontName(fontProfile == null ? "Microsoft YaHei" : fontProfile.getTitleFont());
        labelFont.setBold(true);
        labelFont.setFontHeightInPoints((short) 10);

        Font bodyFont = workbook.createFont();
        bodyFont.setFontName(fontProfile == null ? "DengXian" : fontProfile.getBodyFont());
        bodyFont.setFontHeightInPoints((short) 10);

        Font bodyBoldFont = workbook.createFont();
        bodyBoldFont.setFontName(fontProfile == null ? "DengXian" : fontProfile.getBodyFont());
        bodyBoldFont.setBold(true);
        bodyBoldFont.setFontHeightInPoints((short) 10);

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.CENTER);
        title.setVerticalAlignment(VerticalAlignment.CENTER);
        title.setFillForegroundColor(TITLE_BG.getIndex());
        title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(title);

        CellStyle section = workbook.createCellStyle();
        section.setFont(labelFont);
        section.setVerticalAlignment(VerticalAlignment.CENTER);
        section.setFillForegroundColor(SECTION_BG.getIndex());
        section.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(section);

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setWrapText(true);
        header.setFillForegroundColor(HEADER_BG.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(header);

        CellStyle label = workbook.createCellStyle();
        label.setFont(labelFont);
        label.setVerticalAlignment(VerticalAlignment.CENTER);
        label.setFillForegroundColor(LABEL_BG.getIndex());
        label.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(label);

        CellStyle body = workbook.createCellStyle();
        body.setFont(bodyFont);
        body.setVerticalAlignment(VerticalAlignment.TOP);
        body.setWrapText(true);
        applyBorder(body);

        CellStyle bodyStrong = workbook.createCellStyle();
        bodyStrong.cloneStyleFrom(body);
        bodyStrong.setFont(bodyBoldFont);

        return new Styles(title, section, header, label, body, bodyStrong);
    }

    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    /**
     * Excel 样式集合。
     *
     * @author mumu
     * @date 2026-03-30
     */
    private record Styles(CellStyle title,
                          CellStyle section,
                          CellStyle header,
                          CellStyle label,
                          CellStyle body,
                          CellStyle bodyStrong) {
    }
}
