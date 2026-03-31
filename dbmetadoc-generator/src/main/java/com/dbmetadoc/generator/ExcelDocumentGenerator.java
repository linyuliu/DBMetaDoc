package com.dbmetadoc.generator;

import cn.hutool.core.collection.CollUtil;
import com.dbmetadoc.generator.model.DocumentColumnModel;
import com.dbmetadoc.generator.model.DocumentForeignKeyModel;
import com.dbmetadoc.generator.model.DocumentIndexModel;
import com.dbmetadoc.generator.model.DocumentTableModel;
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

    private static final int DEFAULT_ROW_HEIGHT = 420;
    private static final int TITLE_ROW_HEIGHT = 560;
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
        if (Boolean.TRUE.equals(view.getShowTableOverview()) && CollUtil.isNotEmpty(view.getTableOverviewRows())) {
            rowIndex++;
            rowIndex = writeSectionTitle(sheet, rowIndex, 0, 2, "表目录", styles.section);
            rowIndex = writeHeaderRow(sheet, rowIndex, List.of("序号", "表名", "表说明"), styles.header);
            for (DocumentTableModel table : view.getTableOverviewRows()) {
                rowIndex = writeValuesRow(sheet, rowIndex, List.of(
                        String.valueOf(table.getTableNo()),
                        (table.getSchema() == null || table.getSchema().isBlank() ? "" : table.getSchema() + ".") + table.getName(),
                        table.getComment()
                ), styles.body);
            }
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
                rowIndex = writeSectionTitle(sheet, rowIndex, 0, 5, "字段清单", styles.section);
                rowIndex = writeHeaderRow(sheet, rowIndex, List.of("字段名", "类型", "主键", "可空", "默认值", "注释"), styles.header);
                for (DocumentColumnModel column : table.getColumns()) {
                    rowIndex = writeRow(sheet, rowIndex, List.of(
                            column.getName(),
                            column.getType(),
                            column.getPrimaryKeyText(),
                            column.getNullableText(),
                            column.getDefaultValue(),
                            column.getComment()
                    ), styles);
                }
            }

            if (Boolean.TRUE.equals(table.getHasExtendedColumns())) {
                rowIndex++;
                rowIndex = writeSectionTitle(sheet, rowIndex, 0, 4, "字段扩展补充", styles.section);
                rowIndex = writeHeaderRow(sheet, rowIndex, List.of("序号", "字段名", "原始类型", "Java 类型", "扩展说明"), styles.header);
                for (DocumentColumnModel column : table.getExtendedColumns()) {
                    rowIndex = writeRow(sheet, rowIndex, List.of(
                            String.valueOf(column.getOrderNo()),
                            column.getName(),
                            column.getRawType(),
                            column.getJavaType(),
                            column.getExtendedSummary()
                    ), styles);
                }
            }

            if (Boolean.TRUE.equals(table.getHasIndexes())) {
                rowIndex++;
                rowIndex = writeSectionTitle(sheet, rowIndex, 0, 3, "索引信息", styles.section);
                rowIndex = writeHeaderRow(sheet, rowIndex, List.of("索引名", "包含字段", "唯一", "类型"), styles.header);
                for (DocumentIndexModel index : table.getIndexes()) {
                    rowIndex = writeRow(sheet, rowIndex, List.of(
                            index.getName(),
                            index.getColumnNamesText(),
                            index.getUniqueText(),
                            index.getType()
                    ), styles);
                }
            }

            if (Boolean.TRUE.equals(table.getHasForeignKeys())) {
                rowIndex++;
                rowIndex = writeSectionTitle(sheet, rowIndex, 0, 3, "外键信息", styles.section);
                rowIndex = writeHeaderRow(sheet, rowIndex, List.of("外键名", "本表字段", "引用表", "引用字段"), styles.header);
                for (DocumentForeignKeyModel foreignKey : table.getForeignKeys()) {
                    rowIndex = writeRow(sheet, rowIndex, List.of(
                            foreignKey.getName(),
                            foreignKey.getColumnName(),
                            foreignKey.getReferencedTable(),
                            foreignKey.getReferencedColumn()
                    ), styles);
                }
            }

            setColumnWidths(sheet, 22, 22, 12, 12, 18, 30, 18);
            sheet.createFreezePane(0, 1);
        }
    }

    private int writeMergedTitle(Sheet sheet, int rowIndex, int firstCol, int lastCol, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeight((short) TITLE_ROW_HEIGHT);
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
        row.setHeight((short) DEFAULT_ROW_HEIGHT);
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
        row.setHeight((short) DEFAULT_ROW_HEIGHT);
        createCell(row, 0, key, styles.label);
        createCell(row, 1, value, styles.body);
        return rowIndex + 1;
    }

    private int writeHeaderRow(Sheet sheet, int rowIndex, List<String> headers, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeight((short) DEFAULT_ROW_HEIGHT);
        for (int index = 0; index < headers.size(); index++) {
            createCell(row, index, headers.get(index), style);
        }
        return rowIndex + 1;
    }

    private int writeValuesRow(Sheet sheet, int rowIndex, List<String> values, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeight((short) DEFAULT_ROW_HEIGHT);
        for (int index = 0; index < values.size(); index++) {
            createCell(row, index, values.get(index), style);
        }
        return rowIndex + 1;
    }

    private int writeRow(Sheet sheet, int rowIndex, List<String> values, Styles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeight((short) DEFAULT_ROW_HEIGHT);
        for (int index = 0; index < values.size(); index++) {
            CellStyle style = index > 0 && index < 3 ? styles.mono : styles.body;
            createCell(row, index, values.get(index), style);
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

    private Styles createStyles(XSSFWorkbook workbook, FontRenderProfile fontProfile) {
        Font titleFont = workbook.createFont();
        titleFont.setFontName(fontProfile == null ? "Microsoft YaHei" : fontProfile.getTitleFont());
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font headerFont = workbook.createFont();
        headerFont.setFontName(fontProfile == null ? "Microsoft YaHei" : fontProfile.getTitleFont());
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 10);

        Font labelFont = workbook.createFont();
        labelFont.setFontName(fontProfile == null ? "Microsoft YaHei" : fontProfile.getTitleFont());
        labelFont.setBold(true);
        labelFont.setFontHeightInPoints((short) 10);

        Font bodyFont = workbook.createFont();
        bodyFont.setFontName(fontProfile == null ? "DengXian" : fontProfile.getBodyFont());
        bodyFont.setFontHeightInPoints((short) 10);

        Font monoFont = workbook.createFont();
        monoFont.setFontName(fontProfile == null ? "Cascadia Mono" : fontProfile.getMonoFont());
        monoFont.setFontHeightInPoints((short) 10);

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

        CellStyle mono = workbook.createCellStyle();
        mono.cloneStyleFrom(body);
        mono.setFont(monoFont);

        return new Styles(title, section, header, label, body, mono);
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
                          CellStyle mono) {
    }
}
