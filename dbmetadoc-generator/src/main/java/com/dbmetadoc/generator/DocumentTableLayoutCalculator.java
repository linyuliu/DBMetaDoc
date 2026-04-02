package com.dbmetadoc.generator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.generator.model.DocumentColumnModel;
import com.dbmetadoc.generator.model.DocumentForeignKeyModel;
import com.dbmetadoc.generator.model.DocumentIndexModel;
import com.dbmetadoc.generator.model.DocumentTableColumnLayout;
import com.dbmetadoc.generator.model.DocumentTableLayout;
import com.dbmetadoc.generator.support.GeneratorSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 表格布局计算器。
 *
 * @author mumu
 * @date 2026-03-31
 */
public final class DocumentTableLayoutCalculator {

    private static final int MAX_ROW_LINES = 4;
    private static final double CJK_WEIGHT = 1.0d;
    private static final double LETTER_DIGIT_WEIGHT = 0.62d;
    private static final double SYMBOL_WEIGHT = 0.45d;
    private static final double SPACE_WEIGHT = 0.35d;
    private static final double OTHER_WEIGHT = 0.7d;

    private DocumentTableLayoutCalculator() {
    }

    public static DocumentTableLayout buildBasicColumnLayout(List<DocumentColumnModel> rows) {
        return calculate(List.of(
                spec("orderNo", "序号", 4, 5, row -> String.valueOf(row.getOrderNo())),
                spec("name", "列名", 10, 18, DocumentColumnModel::getName),
                spec("type", "数据类型", 9, 15, DocumentColumnModel::getType),
                spec("primaryKeyText", "主键", 5, 6, DocumentColumnModel::getPrimaryKeyText),
                spec("nullableText", "可空", 5, 6, DocumentColumnModel::getNullableText),
                spec("defaultValue", "默认值", 10, 16, DocumentColumnModel::getDefaultValue),
                spec("comment", "列说明", 14, 34, DocumentColumnModel::getComment)
        ), rows);
    }

    public static DocumentTableLayout buildExtendedColumnLayout(List<DocumentColumnModel> rows) {
        return calculate(List.of(
                spec("orderNo", "序号", 4, 5, row -> String.valueOf(row.getOrderNo())),
                spec("name", "字段名", 10, 16, DocumentColumnModel::getName),
                spec("rawType", "原始类型", 12, 18, DocumentColumnModel::getRawType),
                spec("javaType", "Java 类型", 10, 14, DocumentColumnModel::getJavaType),
                spec("extendedSummary", "扩展说明", 16, 30, DocumentColumnModel::getExtendedSummary)
        ), rows);
    }

    public static DocumentTableLayout buildIndexLayout(List<DocumentIndexModel> rows) {
        return calculate(List.of(
                spec("name", "索引名", 12, 20, DocumentIndexModel::getName),
                spec("columnNamesText", "包含字段", 18, 32, DocumentIndexModel::getColumnNamesText),
                spec("uniqueText", "唯一", 5, 6, DocumentIndexModel::getUniqueText),
                spec("type", "类型", 8, 12, DocumentIndexModel::getType)
        ), rows);
    }

    public static DocumentTableLayout buildForeignKeyLayout(List<DocumentForeignKeyModel> rows) {
        return calculate(List.of(
                spec("name", "外键名", 12, 20, DocumentForeignKeyModel::getName),
                spec("columnName", "本表字段", 8, 14, DocumentForeignKeyModel::getColumnName),
                spec("referencedTable", "引用表", 16, 28, DocumentForeignKeyModel::getReferencedTable),
                spec("referencedColumn", "引用字段", 8, 14, DocumentForeignKeyModel::getReferencedColumn)
        ), rows);
    }

    public static int toWordWidth(double widthRatio, int totalWidth) {
        return Math.max(1, (int) Math.round(widthRatio * totalWidth));
    }

    public static int toExcelWidth(double characterBudget) {
        return Math.max(8, Math.min(255, (int) Math.ceil(characterBudget + 2)));
    }

    private static <T> DocumentTableLayout calculate(List<ColumnSpec<T>> columnSpecs, List<T> rows) {
        List<T> safeRows = CollUtil.emptyIfNull(rows);
        List<Double> characterBudgets = new ArrayList<>(columnSpecs.size());
        double totalBudget = 0d;
        for (ColumnSpec<T> spec : columnSpecs) {
            double maxContentLength = safeRows.stream()
                    .map(spec.extractor())
                    .mapToDouble(DocumentTableLayoutCalculator::visualLength)
                    .max()
                    .orElse(0d);
            double targetBudget = Math.max(visualLength(spec.header()), maxContentLength);
            double clampedBudget = clamp(targetBudget, spec.minCharacters(), spec.maxCharacters());
            characterBudgets.add(clampedBudget);
            totalBudget += clampedBudget;
        }
        if (totalBudget <= 0d) {
            totalBudget = columnSpecs.size();
        }

        List<DocumentTableColumnLayout> columns = new ArrayList<>(columnSpecs.size());
        for (int index = 0; index < columnSpecs.size(); index++) {
            ColumnSpec<T> spec = columnSpecs.get(index);
            double characterBudget = characterBudgets.get(index);
            double widthRatio = characterBudget / totalBudget;
            columns.add(DocumentTableColumnLayout.builder()
                    .key(spec.key())
                    .header(spec.header())
                    .characterBudget(characterBudget)
                    .widthRatio(widthRatio)
                    .htmlWidthPercent(String.format(Locale.ROOT, "%.4f%%", widthRatio * 100d))
                    .build());
        }

        List<Integer> rowLineCounts = safeRows.stream()
                .map(row -> estimateRowLines(columnSpecs, columns, row))
                .toList();
        List<String> rowClasses = rowLineCounts.stream()
                .map(lineCount -> "row-lines-" + Math.min(lineCount, MAX_ROW_LINES))
                .toList();
        return DocumentTableLayout.builder()
                .columns(columns)
                .rowLineCounts(rowLineCounts)
                .rowClasses(rowClasses)
                .build();
    }

    private static <T> int estimateRowLines(List<ColumnSpec<T>> columnSpecs,
                                            List<DocumentTableColumnLayout> columns,
                                            T row) {
        int maxLines = 1;
        for (int index = 0; index < columnSpecs.size(); index++) {
            String value = columnSpecs.get(index).extractor().apply(row);
            double budget = columns.get(index).getCharacterBudget();
            int lineCount = estimateCellLines(value, budget);
            maxLines = Math.max(maxLines, lineCount);
        }
        return Math.min(maxLines, MAX_ROW_LINES);
    }

    private static int estimateCellLines(String value, double characterBudget) {
        if (characterBudget <= 0d) {
            return 1;
        }
        double effectiveLength = visualLength(value);
        if (effectiveLength <= 0d) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(effectiveLength / characterBudget));
    }

    private static double visualLength(String value) {
        if (StrUtil.isBlank(value)) {
            return 0d;
        }
        double total = 0d;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            total += weightOf(codePoint);
            offset += Character.charCount(codePoint);
        }
        return total;
    }

    private static double weightOf(int codePoint) {
        if (Character.isWhitespace(codePoint)) {
            return SPACE_WEIGHT;
        }
        if (isCjk(codePoint)) {
            return CJK_WEIGHT;
        }
        if (Character.isLetterOrDigit(codePoint)) {
            return LETTER_DIGIT_WEIGHT;
        }
        if (codePoint == '_' || codePoint == '.' || codePoint == '-' || codePoint == '(' || codePoint == ')'
                || codePoint == '[' || codePoint == ']' || codePoint == '{' || codePoint == '}'
                || codePoint == '/' || codePoint == '\\' || codePoint == ',') {
            return SYMBOL_WEIGHT;
        }
        return OTHER_WEIGHT;
    }

    private static boolean isCjk(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA;
    }

    private static double clamp(double value, int minValue, int maxValue) {
        return Math.max(minValue, Math.min(value, maxValue));
    }

    private static <T> ColumnSpec<T> spec(String key, String header, int minCharacters, int maxCharacters,
                                          Function<T, String> extractor) {
        return new ColumnSpec<>(key, header, minCharacters, maxCharacters,
                row -> GeneratorSupport.safeText(extractor.apply(row)));
    }

    private record ColumnSpec<T>(String key,
                                 String header,
                                 int minCharacters,
                                 int maxCharacters,
                                 Function<T, String> extractor) {
    }
}
