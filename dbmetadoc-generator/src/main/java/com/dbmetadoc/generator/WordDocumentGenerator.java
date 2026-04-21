package com.dbmetadoc.generator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.generator.model.DocumentColumnModel;
import com.dbmetadoc.generator.model.DocumentForeignKeyModel;
import com.dbmetadoc.generator.model.DocumentIndexModel;
import com.dbmetadoc.generator.model.DocumentTableModel;
import com.dbmetadoc.generator.model.DocumentTableLayout;
import com.dbmetadoc.generator.model.DocumentTemplateModel;
import com.dbmetadoc.generator.support.GeneratorSupport;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.config.ConfigureBuilder;
import com.deepoove.poi.data.CellRenderData;
import com.deepoove.poi.data.Cells;
import com.deepoove.poi.data.RowRenderData;
import com.deepoove.poi.data.Rows;
import com.deepoove.poi.data.Texts;
import com.deepoove.poi.policy.DynamicTableRenderPolicy;
import com.deepoove.poi.policy.TableRenderPolicy;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Word 文档生成器。
 * <p>
 * Word 输出统一走 poi-tl 模板渲染，模板负责章节结构与循环块，
 * 复杂表格的列宽、行高和正文样式仍在渲染后由 Apache POI 精调。
 * </p>
 *
 * @author mumu
 * @date 2026-04-01
 */
public class WordDocumentGenerator implements DocumentGenerator {

    private static final String TEMPLATE_PATH = "/templates/word/database-template.docx";
    private static final long A4_WIDTH_TWIPS = 11906L;
    private static final long A4_HEIGHT_TWIPS = 16838L;
    private static final int PAGE_MARGIN_TWIPS = 720;
    private static final int PAGE_TOP_BOTTOM_TWIPS = 900;
    private static final int TABLE_CONTENT_WIDTH_TWIPS = (int) (A4_HEIGHT_TWIPS - PAGE_MARGIN_TWIPS * 2L);
    private static final String COLOR_TITLE = "1F2F45";
    private static final String COLOR_SUBTITLE = "607287";
    private static final String COLOR_BORDER = "D7DDEA";
    private static final String COLOR_HEADER = "F4F7FB";
    private static final String COLOR_STRIPE = "FAFBFD";
    private static final String COLOR_TEXT = "000000";
    private static final String EMPTY_TABLE_MESSAGE = "当前未查询到可导出的表结构。";
    private static final String LEGACY_TITLE_PREFIX = "三.";
    private static final String LEGACY_BASIC_HINT = "主字段表固定保留 6 列核心字段，适合中文 A4 文档阅读和打印。";
    private static final String LEGACY_EXTENDED_HINT = "扩展信息以下方补充区展示，不再额外扩宽主字段表。";
    private static final String LEGACY_BASIC_TITLE = "核心字段清单";
    private static final String BASIC_TITLE = "字段清单";

    @Override
    public String getFormat() {
        return "WORD";
    }

    @Override
    public byte[] generate(DocumentRenderContext renderContext) throws Exception {
        DocumentTemplateModel view = DocumentTemplateModelFactory.create(renderContext);
        FontPalette fonts = FontPalette.from(renderContext.getFontProfile());
        Configure configure = buildConfigure(fonts);
        try (InputStream templateStream = openTemplateStream();
             XWPFTemplate template = XWPFTemplate.compile(templateStream, configure);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            template.render(view);
            XWPFDocument document = template.getXWPFDocument();
            applyPageLayout(document);
            normalizeTemplateCopy(document, view, fonts);
            template.write(outputStream);
            return outputStream.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "Word 文档生成失败: " + ex.getMessage(), ex);
        }
    }

    private Configure buildConfigure(FontPalette fonts) {
        ConfigureBuilder builder = Configure.builder();
        builder.bind("tableOverviewRows", new TableOverviewPolicy(fonts));
        builder.bind("columns", new BasicColumnPolicy(fonts));
        builder.bind("extendedColumns", new ExtendedColumnPolicy(fonts));
        builder.bind("indexes", new IndexPolicy(fonts));
        builder.bind("foreignKeys", new ForeignKeyPolicy(fonts));
        return builder.build();
    }

    private InputStream openTemplateStream() {
        InputStream templateStream = WordDocumentGenerator.class.getResourceAsStream(TEMPLATE_PATH);
        if (templateStream == null) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "未找到 Word 模板资源: " + TEMPLATE_PATH);
        }
        return templateStream;
    }

    private void applyPageLayout(XWPFDocument document) {
        CTSectPr sectionProperties = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectionProperties.isSetPgSz() ? sectionProperties.getPgSz() : sectionProperties.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(A4_HEIGHT_TWIPS));
        pageSize.setH(BigInteger.valueOf(A4_WIDTH_TWIPS));
        pageSize.setOrient(STPageOrientation.LANDSCAPE);

        CTPageMar pageMargin = sectionProperties.isSetPgMar() ? sectionProperties.getPgMar() : sectionProperties.addNewPgMar();
        pageMargin.setTop(BigInteger.valueOf(PAGE_TOP_BOTTOM_TWIPS));
        pageMargin.setBottom(BigInteger.valueOf(PAGE_TOP_BOTTOM_TWIPS));
        pageMargin.setLeft(BigInteger.valueOf(PAGE_MARGIN_TWIPS));
        pageMargin.setRight(BigInteger.valueOf(PAGE_MARGIN_TWIPS));
        pageMargin.setHeader(BigInteger.valueOf(420));
        pageMargin.setFooter(BigInteger.valueOf(420));
    }

    private void normalizeTemplateCopy(XWPFDocument document, DocumentTemplateModel view, FontPalette fonts) {
        rewriteSectionTitles(document, view, fonts);
        stripLegacyHelperParagraphs(document);
        if (!GeneratorSupport.hasItems(view.getTables())) {
            appendEmptyTableMessage(document, fonts);
        }
    }

    private void rewriteSectionTitles(XWPFDocument document, DocumentTemplateModel view, FontPalette fonts) {
        List<XWPFParagraph> titleParagraphs = document.getParagraphs().stream()
                .filter(paragraph -> StrUtil.startWith(paragraph.getText(), LEGACY_TITLE_PREFIX))
                .toList();
        int limit = Math.min(titleParagraphs.size(), CollUtil.size(view.getTables()));
        for (int index = 0; index < limit; index++) {
            rewriteChapterTitle(titleParagraphs.get(index), view.getTables().get(index), fonts);
        }
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (LEGACY_BASIC_TITLE.equals(paragraph.getText())) {
                replaceParagraphText(paragraph, BASIC_TITLE, fonts.title, 11, true, COLOR_TITLE, ParagraphAlignment.LEFT);
            }
        }
    }

    private void rewriteChapterTitle(XWPFParagraph paragraph, DocumentTableModel table, FontPalette fonts) {
        clearParagraph(paragraph);
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        paragraph.setSpacingBefore(120);
        paragraph.setSpacingAfter(90);

        XWPFRun titleRun = paragraph.createRun();
        configureRun(titleRun, table.getChapterTitle(), fonts.title, 13, true, COLOR_TITLE);
    }

    private void stripLegacyHelperParagraphs(XWPFDocument document) {
        List<Integer> removeIndexes = new ArrayList<>();
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            String text = StrUtil.trim(paragraph.getText());
            if (StrUtil.equalsAny(text, LEGACY_BASIC_HINT, LEGACY_EXTENDED_HINT)) {
                removeIndexes.add(document.getPosOfParagraph(paragraph));
            }
        }
        for (int index = removeIndexes.size() - 1; index >= 0; index--) {
            document.removeBodyElement(removeIndexes.get(index));
        }
    }

    private void appendEmptyTableMessage(XWPFDocument document, FontPalette fonts) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingBefore(120);
        paragraph.setSpacingAfter(120);
        XWPFRun run = paragraph.createRun();
        configureRun(run, EMPTY_TABLE_MESSAGE, fonts.body, 11, true, COLOR_SUBTITLE);
    }

    private void replaceParagraphText(XWPFParagraph paragraph,
                                      String text,
                                      String fontFamily,
                                      int fontSize,
                                      boolean bold,
                                      String color,
                                      ParagraphAlignment alignment) {
        clearParagraph(paragraph);
        paragraph.setAlignment(alignment);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        configureRun(run, text, fontFamily, fontSize, bold, color);
    }

    private void clearParagraph(XWPFParagraph paragraph) {
        while (paragraph.getRuns().size() > 0) {
            paragraph.removeRun(0);
        }
    }

    private void configureRun(XWPFRun run, String text, String fontFamily, int fontSize, boolean bold, String color) {
        run.setText(StrUtil.blankToDefault(text, ""));
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setColor(color);
        run.setFontFamily(fontFamily);
        CTFonts fonts = run.getCTR().isSetRPr() && run.getCTR().getRPr().sizeOfRFontsArray() > 0
                ? run.getCTR().getRPr().getRFontsArray(0)
                : (run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr()).addNewRFonts();
        fonts.setAscii(fontFamily);
        fonts.setHAnsi(fontFamily);
        fonts.setEastAsia(fontFamily);
        fonts.setCs(fontFamily);
    }

    /**
     * Word 字体方案。
     */
    private record FontPalette(String title, String body, String mono, String symbol) {

        private static final String DEFAULT_TITLE = "Source Han Sans CN";
        private static final String DEFAULT_BODY = "Source Han Sans CN";
        private static final String DEFAULT_MONO = "JetBrains Mono";
        private static final String DEFAULT_SYMBOL = "Noto Sans SC";

        private static FontPalette from(FontRenderProfile fontProfile) {
            if (fontProfile == null) {
                return new FontPalette(DEFAULT_TITLE, DEFAULT_BODY, DEFAULT_MONO, DEFAULT_SYMBOL);
            }
            return new FontPalette(
                    StrUtil.blankToDefault(fontProfile.getTitleFont(), DEFAULT_TITLE),
                    StrUtil.blankToDefault(fontProfile.getBodyFont(), DEFAULT_BODY),
                    StrUtil.blankToDefault(fontProfile.getMonoFont(), DEFAULT_MONO),
                    StrUtil.blankToDefault(fontProfile.getSymbolFont(), DEFAULT_SYMBOL)
            );
        }
    }

    /**
     * Word 表格列定义。
     */
    private record WordColumn<T>(String header,
                                 ParagraphAlignment alignment,
                                 Function<FontPalette, String> fontSelector,
                                 BiFunction<T, Integer, String> textExtractor) {
    }

    /**
     * 单张 Word 表格的布局结果，避免对同一批数据重复计算列宽和行高。
     */
    private record TableMetrics(List<Integer> widths, List<Integer> rowLineCounts) {
    }

    /**
     * 复用模板中的表格定位，但表格内容统一重建为当前导出逻辑。
     */
    private abstract static class AbstractWordTablePolicy<T> extends DynamicTableRenderPolicy {

        private final FontPalette fonts;

        protected AbstractWordTablePolicy(FontPalette fonts) {
            this.fonts = fonts;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void render(XWPFTable table, Object data) throws Exception {
            List<T> rows = data instanceof List<?> values ? (List<T>) values : List.of();
            rebuildTable(table, rows);
        }

        protected abstract List<WordColumn<T>> columns();

        protected abstract List<Integer> resolveWordWidths(List<T> rows);

        protected abstract int resolveRowLines(List<T> rows, int rowIndex);

        protected TableMetrics prepareMetrics(List<T> rows) {
            List<Integer> rowLineCounts = new ArrayList<>(rows.size());
            for (int index = 0; index < rows.size(); index++) {
                rowLineCounts.add(resolveRowLines(rows, index));
            }
            return new TableMetrics(resolveWordWidths(rows), rowLineCounts);
        }

        protected void rebuildTable(XWPFTable table, List<T> rows) throws Exception {
            configureTable(table);
            clearRows(table);
            List<WordColumn<T>> columns = columns();
            TableMetrics metrics = prepareMetrics(rows);

            XWPFTableRow headerRow = appendRow(table, columns.size());
            TableRenderPolicy.Helper.renderRow(headerRow, buildHeaderRow(columns));
            styleRow(headerRow, 1);
            headerRow.setRepeatHeader(true);
            applyCellWidths(headerRow, metrics.widths());

            for (int index = 0; index < rows.size(); index++) {
                XWPFTableRow row = appendRow(table, columns.size());
                TableRenderPolicy.Helper.renderRow(row, buildBodyRow(columns, rows.get(index), index));
                styleRow(row, safeRowLineCount(metrics, index));
                applyCellWidths(row, metrics.widths());
            }
        }

        private RowRenderData buildHeaderRow(List<WordColumn<T>> columns) {
            Rows.RowBuilder builder = Rows.of();
            for (WordColumn<T> column : columns) {
                builder.addCell(cell(column.header(), fonts.title, 10, true, column.alignment(), COLOR_HEADER, COLOR_TITLE));
            }
            return builder.create();
        }

        private RowRenderData buildBodyRow(List<WordColumn<T>> columns, T row, int rowIndex) {
            String backgroundColor = rowIndex % 2 == 0 ? "FFFFFF" : COLOR_STRIPE;
            Rows.RowBuilder builder = Rows.of();
            for (WordColumn<T> column : columns) {
                String text = GeneratorSupport.safeText(column.textExtractor().apply(row, rowIndex));
                builder.addCell(cell(text, column.fontSelector().apply(fonts), 9, true, column.alignment(), backgroundColor, COLOR_TEXT));
            }
            return builder.create();
        }

        private CellRenderData cell(String text,
                                    String fontFamily,
                                    double fontSize,
                                    boolean bold,
                                    ParagraphAlignment alignment,
                                    String backgroundColor,
                                    String color) {
            var textBuilder = Texts.of(text).fontFamily(fontFamily).fontSize(fontSize).color(color);
            if (bold) {
                textBuilder.bold();
            }
            Cells.CellBuilder builder = Cells.of(textBuilder.create()).bgColor(backgroundColor).verticalCenter();
            switch (alignment) {
                case CENTER -> builder.horizontalCenter();
                case RIGHT -> builder.horizontalRight();
                default -> builder.horizontalLeft();
            }
            return builder.create();
        }

        private void clearRows(XWPFTable table) {
            while (table.getNumberOfRows() > 0) {
                table.removeRow(0);
            }
        }

        private XWPFTableRow appendRow(XWPFTable table, int cellCount) {
            XWPFTableRow row = table.insertNewTableRow(table.getNumberOfRows());
            for (int index = 0; index < cellCount; index++) {
                row.createCell();
            }
            return row;
        }

        private void configureTable(XWPFTable table) {
            table.setTableAlignment(TableRowAlign.CENTER);
            table.setCellMargins(40, 70, 40, 70);
            CTTblPr tableProperties = table.getCTTbl().getTblPr() == null
                    ? table.getCTTbl().addNewTblPr()
                    : table.getCTTbl().getTblPr();
            CTTblLayoutType layoutType = tableProperties.isSetTblLayout()
                    ? tableProperties.getTblLayout()
                    : tableProperties.addNewTblLayout();
            layoutType.setType(STTblLayoutType.FIXED);
            CTTblWidth tableWidth = tableProperties.isSetTblW() ? tableProperties.getTblW() : tableProperties.addNewTblW();
            tableWidth.setType(STTblWidth.DXA);
            tableWidth.setW(BigInteger.valueOf(TABLE_CONTENT_WIDTH_TWIPS));
            table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
            table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
            table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
            table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
            table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
            table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, COLOR_BORDER);
        }

        private void applyCellWidths(XWPFTableRow row, List<Integer> widths) {
            for (int index = 0; index < widths.size() && index < row.getTableCells().size(); index++) {
                XWPFTableCell cell = row.getCell(index);
                CTTcPr cellProperties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
                CTTblWidth cellWidth = cellProperties.isSetTcW() ? cellProperties.getTcW() : cellProperties.addNewTcW();
                cellWidth.setType(STTblWidth.DXA);
                cellWidth.setW(BigInteger.valueOf(widths.get(index)));
            }
        }

        private void styleRow(XWPFTableRow row, int lineCount) {
            row.setHeight(rowHeightTwips(lineCount));
            for (XWPFTableCell cell : row.getTableCells()) {
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        private int rowHeightTwips(int lineCount) {
            return switch (Math.max(1, Math.min(lineCount, 4))) {
                case 1 -> 360;
                case 2 -> 560;
                case 3 -> 760;
                default -> 960;
            };
        }

        private int safeRowLineCount(TableMetrics metrics, int rowIndex) {
            if (metrics == null || metrics.rowLineCounts() == null || rowIndex < 0 || rowIndex >= metrics.rowLineCounts().size()) {
                return 1;
            }
            return metrics.rowLineCounts().get(rowIndex);
        }
    }

    /**
     * 表概览表格。
     */
    private static final class TableOverviewPolicy extends AbstractWordTablePolicy<DocumentTableModel> {

        private TableOverviewPolicy(FontPalette fonts) {
            super(fonts);
        }

        @Override
        protected List<WordColumn<DocumentTableModel>> columns() {
            return List.of(
                    new WordColumn<>("序号", ParagraphAlignment.CENTER, FontPalette::body,
                            (table, rowIndex) -> Convert.toStr(table.getTableNo())),
                    new WordColumn<>("表名", ParagraphAlignment.LEFT, FontPalette::mono,
                            (table, rowIndex) -> GeneratorSupport.safeText(table.getName())),
                    new WordColumn<>("Schema", ParagraphAlignment.CENTER, FontPalette::mono,
                            (table, rowIndex) -> GeneratorSupport.safeText(table.getSchema())),
                    new WordColumn<>("列数", ParagraphAlignment.CENTER, FontPalette::body,
                            (table, rowIndex) -> Convert.toStr(table.getColumnCount())),
                    new WordColumn<>("注释", ParagraphAlignment.LEFT, FontPalette::body,
                            (table, rowIndex) -> GeneratorSupport.defaultText(table.getComment(), "未填写表注释"))
            );
        }

        @Override
        protected List<Integer> resolveWordWidths(List<DocumentTableModel> rows) {
            return budgetsToWidths(4d, 14d, 8d, 5d, 25d);
        }

        @Override
        protected int resolveRowLines(List<DocumentTableModel> rows, int rowIndex) {
            return estimateLines(GeneratorSupport.defaultText(rows.get(rowIndex).getComment(), ""), 25d);
        }
    }

    /**
     * 基础字段表格。
     */
    private static final class BasicColumnPolicy extends AbstractWordTablePolicy<DocumentColumnModel> {

        private BasicColumnPolicy(FontPalette fonts) {
            super(fonts);
        }

        @Override
        protected List<WordColumn<DocumentColumnModel>> columns() {
            return List.of(
                    new WordColumn<>("序号", ParagraphAlignment.CENTER, FontPalette::body,
                            (column, rowIndex) -> Convert.toStr(column.getOrderNo())),
                    new WordColumn<>("列名", ParagraphAlignment.LEFT, FontPalette::mono,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getName())),
                    new WordColumn<>("数据类型", ParagraphAlignment.LEFT, FontPalette::mono,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getType())),
                    new WordColumn<>("主键", ParagraphAlignment.CENTER, FontPalette::symbol,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getPrimaryKeyText())),
                    new WordColumn<>("可空", ParagraphAlignment.CENTER, FontPalette::symbol,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getNullableText())),
                    new WordColumn<>("默认值", ParagraphAlignment.LEFT, FontPalette::body,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getDefaultValue())),
                    new WordColumn<>("列说明", ParagraphAlignment.LEFT, FontPalette::body,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getComment()))
            );
        }

        @Override
        protected List<Integer> resolveWordWidths(List<DocumentColumnModel> rows) {
            return layoutToWordWidths(DocumentTableLayoutCalculator.buildBasicColumnLayout(rows));
        }

        @Override
        protected int resolveRowLines(List<DocumentColumnModel> rows, int rowIndex) {
            return rowLines(DocumentTableLayoutCalculator.buildBasicColumnLayout(rows), rowIndex);
        }

        @Override
        protected TableMetrics prepareMetrics(List<DocumentColumnModel> rows) {
            DocumentTableLayout layout = DocumentTableLayoutCalculator.buildBasicColumnLayout(rows);
            return new TableMetrics(layoutToWordWidths(layout), layoutRowLines(layout, rows.size()));
        }
    }

    /**
     * 扩展字段表格。
     */
    private static final class ExtendedColumnPolicy extends AbstractWordTablePolicy<DocumentColumnModel> {

        private ExtendedColumnPolicy(FontPalette fonts) {
            super(fonts);
        }

        @Override
        protected List<WordColumn<DocumentColumnModel>> columns() {
            return List.of(
                    new WordColumn<>("序号", ParagraphAlignment.CENTER, FontPalette::body,
                            (column, rowIndex) -> Convert.toStr(column.getOrderNo())),
                    new WordColumn<>("字段名", ParagraphAlignment.LEFT, FontPalette::mono,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getName())),
                    new WordColumn<>("原始类型", ParagraphAlignment.LEFT, FontPalette::mono,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getRawType())),
                    new WordColumn<>("Java 类型", ParagraphAlignment.LEFT, FontPalette::mono,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getJavaType())),
                    new WordColumn<>("扩展说明", ParagraphAlignment.LEFT, FontPalette::body,
                            (column, rowIndex) -> GeneratorSupport.safeText(column.getExtendedSummary()))
            );
        }

        @Override
        protected List<Integer> resolveWordWidths(List<DocumentColumnModel> rows) {
            return layoutToWordWidths(DocumentTableLayoutCalculator.buildExtendedColumnLayout(rows));
        }

        @Override
        protected int resolveRowLines(List<DocumentColumnModel> rows, int rowIndex) {
            return rowLines(DocumentTableLayoutCalculator.buildExtendedColumnLayout(rows), rowIndex);
        }

        @Override
        protected TableMetrics prepareMetrics(List<DocumentColumnModel> rows) {
            DocumentTableLayout layout = DocumentTableLayoutCalculator.buildExtendedColumnLayout(rows);
            return new TableMetrics(layoutToWordWidths(layout), layoutRowLines(layout, rows.size()));
        }
    }

    /**
     * 索引表格。
     */
    private static final class IndexPolicy extends AbstractWordTablePolicy<DocumentIndexModel> {

        private IndexPolicy(FontPalette fonts) {
            super(fonts);
        }

        @Override
        protected List<WordColumn<DocumentIndexModel>> columns() {
            return List.of(
                    new WordColumn<>("索引名", ParagraphAlignment.LEFT, FontPalette::mono,
                            (index, rowIndex) -> GeneratorSupport.safeText(index.getName())),
                    new WordColumn<>("包含字段", ParagraphAlignment.LEFT, FontPalette::body,
                            (index, rowIndex) -> GeneratorSupport.safeText(index.getColumnNamesText())),
                    new WordColumn<>("唯一", ParagraphAlignment.CENTER, FontPalette::symbol,
                            (index, rowIndex) -> GeneratorSupport.safeText(index.getUniqueText())),
                    new WordColumn<>("类型", ParagraphAlignment.LEFT, FontPalette::mono,
                            (index, rowIndex) -> GeneratorSupport.safeText(index.getType()))
            );
        }

        @Override
        protected List<Integer> resolveWordWidths(List<DocumentIndexModel> rows) {
            return layoutToWordWidths(DocumentTableLayoutCalculator.buildIndexLayout(rows));
        }

        @Override
        protected int resolveRowLines(List<DocumentIndexModel> rows, int rowIndex) {
            return rowLines(DocumentTableLayoutCalculator.buildIndexLayout(rows), rowIndex);
        }

        @Override
        protected TableMetrics prepareMetrics(List<DocumentIndexModel> rows) {
            DocumentTableLayout layout = DocumentTableLayoutCalculator.buildIndexLayout(rows);
            return new TableMetrics(layoutToWordWidths(layout), layoutRowLines(layout, rows.size()));
        }
    }

    /**
     * 外键表格。
     */
    private static final class ForeignKeyPolicy extends AbstractWordTablePolicy<DocumentForeignKeyModel> {

        private ForeignKeyPolicy(FontPalette fonts) {
            super(fonts);
        }

        @Override
        protected List<WordColumn<DocumentForeignKeyModel>> columns() {
            return List.of(
                    new WordColumn<>("外键名", ParagraphAlignment.LEFT, FontPalette::mono,
                            (foreignKey, rowIndex) -> GeneratorSupport.safeText(foreignKey.getName())),
                    new WordColumn<>("本表字段", ParagraphAlignment.LEFT, FontPalette::mono,
                            (foreignKey, rowIndex) -> GeneratorSupport.safeText(foreignKey.getColumnName())),
                    new WordColumn<>("引用表", ParagraphAlignment.LEFT, FontPalette::mono,
                            (foreignKey, rowIndex) -> GeneratorSupport.safeText(foreignKey.getReferencedTable())),
                    new WordColumn<>("引用字段", ParagraphAlignment.LEFT, FontPalette::mono,
                            (foreignKey, rowIndex) -> GeneratorSupport.safeText(foreignKey.getReferencedColumn()))
            );
        }

        @Override
        protected List<Integer> resolveWordWidths(List<DocumentForeignKeyModel> rows) {
            return layoutToWordWidths(DocumentTableLayoutCalculator.buildForeignKeyLayout(rows));
        }

        @Override
        protected int resolveRowLines(List<DocumentForeignKeyModel> rows, int rowIndex) {
            return rowLines(DocumentTableLayoutCalculator.buildForeignKeyLayout(rows), rowIndex);
        }

        @Override
        protected TableMetrics prepareMetrics(List<DocumentForeignKeyModel> rows) {
            DocumentTableLayout layout = DocumentTableLayoutCalculator.buildForeignKeyLayout(rows);
            return new TableMetrics(layoutToWordWidths(layout), layoutRowLines(layout, rows.size()));
        }
    }

    private static List<Integer> layoutToWordWidths(DocumentTableLayout layout) {
        if (layout == null || CollUtil.isEmpty(layout.getColumns())) {
            return List.of(TABLE_CONTENT_WIDTH_TWIPS);
        }
        List<Integer> widths = new ArrayList<>(layout.getColumns().size());
        int remaining = TABLE_CONTENT_WIDTH_TWIPS;
        for (int index = 0; index < layout.getColumns().size(); index++) {
            int width = index == layout.getColumns().size() - 1
                    ? remaining
                    : DocumentTableLayoutCalculator.toWordWidth(layout.getColumns().get(index).getWidthRatio(), TABLE_CONTENT_WIDTH_TWIPS);
            width = Math.max(1, width);
            widths.add(width);
            remaining -= width;
        }
        return widths;
    }

    private static int rowLines(DocumentTableLayout layout, int rowIndex) {
        if (layout == null || CollUtil.isEmpty(layout.getRowLineCounts()) || rowIndex < 0 || rowIndex >= layout.getRowLineCounts().size()) {
            return 1;
        }
        return layout.getRowLineCounts().get(rowIndex);
    }

    private static List<Integer> layoutRowLines(DocumentTableLayout layout, int expectedSize) {
        if (layout == null || CollUtil.isEmpty(layout.getRowLineCounts())) {
            List<Integer> defaults = new ArrayList<>(expectedSize);
            for (int index = 0; index < expectedSize; index++) {
                defaults.add(1);
            }
            return defaults;
        }
        return new ArrayList<>(layout.getRowLineCounts());
    }

    private static List<Integer> budgetsToWidths(double... budgets) {
        double total = 0d;
        for (double budget : budgets) {
            total += budget;
        }
        int remaining = TABLE_CONTENT_WIDTH_TWIPS;
        List<Integer> widths = new ArrayList<>(budgets.length);
        for (int index = 0; index < budgets.length; index++) {
            int width = index == budgets.length - 1
                    ? remaining
                    : Math.max(1, (int) Math.round(TABLE_CONTENT_WIDTH_TWIPS * budgets[index] / total));
            widths.add(width);
            remaining -= width;
        }
        return widths;
    }

    private static int estimateLines(String text, double budget) {
        if (budget <= 0d) {
            return 1;
        }
        String value = StrUtil.blankToDefault(text, "");
        double length = 0d;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (Character.isWhitespace(codePoint)) {
                length += 0.35d;
            } else if (Character.isLetterOrDigit(codePoint)) {
                length += 0.62d;
            } else {
                length += 1d;
            }
            offset += Character.charCount(codePoint);
        }
        return Math.max(1, (int) Math.ceil(length / budget));
    }
}
