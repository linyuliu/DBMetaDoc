package com.dbmetadoc.generator;

import cn.hutool.core.collection.CollUtil;
import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;
import com.dbmetadoc.generator.model.DocumentColumnModel;
import com.dbmetadoc.generator.model.DocumentForeignKeyModel;
import com.dbmetadoc.generator.model.DocumentIndexModel;
import com.dbmetadoc.generator.model.DocumentTableModel;
import com.dbmetadoc.generator.model.DocumentTemplateModel;
import com.dbmetadoc.generator.model.DocumentTheme;
import com.dbmetadoc.generator.support.GeneratorSupport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 文档模板视图模型工厂。
 *
 * @author mumu
 * @date 2026-03-30
 */
public final class DocumentTemplateModelFactory {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DocumentTemplateModelFactory() {
    }

    public static DocumentTemplateModel create(DocumentRenderContext renderContext) {
        return create(renderContext, DocumentRenderTarget.HTML_PREVIEW);
    }

    public static DocumentTemplateModel create(DocumentRenderContext renderContext, DocumentRenderTarget renderTarget) {
        DatabaseInfo databaseInfo = renderContext.getDatabase();
        DocumentTheme theme = DocumentThemeFactory.create(renderContext.getFontProfile(), renderTarget);
        List<DocumentTableModel> tables = buildTables(databaseInfo, renderContext);
        String databaseName = GeneratorSupport.defaultText(databaseInfo.getDatabaseName(), databaseInfo.getName());
        int tableCount = CollUtil.size(tables);
        return DocumentTemplateModel.builder()
                .title(GeneratorSupport.defaultText(renderContext.getTitle(), "数据库文档"))
                .subtitle(String.format("%s · 共 %d 张表", databaseName, tableCount))
                .generatedAt(DATE_TIME_FORMATTER.format(LocalDateTime.now()))
                .databaseName(databaseName)
                .type(GeneratorSupport.safeText(databaseInfo.getType()))
                .version(GeneratorSupport.safeText(databaseInfo.getVersion()))
                .schemaName(GeneratorSupport.safeText(databaseInfo.getSchemaName()))
                .catalogName(GeneratorSupport.safeText(databaseInfo.getCatalogName()))
                .charset(GeneratorSupport.safeText(databaseInfo.getCharset()))
                .collation(GeneratorSupport.safeText(databaseInfo.getCollation()))
                .tableCount(tableCount)
                .hasTables(CollUtil.isNotEmpty(tables))
                .showDatabaseOverview(renderContext.hasSection("DATABASE_OVERVIEW"))
                .showTableOverview(renderContext.hasSection("TABLE_OVERVIEW"))
                .showBasicColumns(renderContext.hasSection("COLUMN_BASIC"))
                .showExtendedColumns(renderContext.hasSection("COLUMN_EXTENDED"))
                .showIndexes(renderContext.hasSection("INDEXES"))
                .showForeignKeys(renderContext.hasSection("FOREIGN_KEYS"))
                .tableOverviewRows(tables)
                .tables(tables)
                .theme(theme)
                .build();
    }

    private static List<DocumentTableModel> buildTables(DatabaseInfo databaseInfo, DocumentRenderContext renderContext) {
        if (databaseInfo == null || CollUtil.isEmpty(databaseInfo.getTables())) {
            return List.of();
        }
        String booleanDisplayStyle = renderContext == null ? GeneratorSupport.normalizeBooleanDisplayStyle(null)
                : renderContext.getBooleanDisplayStyle();
        int tableNo = 1;
        List<DocumentTableModel> tables = CollUtil.newArrayList();
        for (TableInfo tableInfo : databaseInfo.getTables()) {
            List<DocumentColumnModel> columns = buildColumns(tableInfo.getColumns(), booleanDisplayStyle);
            List<DocumentIndexModel> indexes = buildIndexes(tableInfo.getIndexes(), booleanDisplayStyle);
            List<DocumentForeignKeyModel> foreignKeys = buildForeignKeys(tableInfo.getForeignKeys());
            String fullTableName = GeneratorSupport.safeText(tableInfo.getSchema())
                    + (GeneratorSupport.hasText(tableInfo.getSchema()) ? "." : "")
                    + GeneratorSupport.safeText(tableInfo.getName());
            tables.add(DocumentTableModel.builder()
                    .tableNo(tableNo)
                    .chapterTitle(tableNo + ". " + fullTableName)
                    .name(GeneratorSupport.safeText(tableInfo.getName()))
                    .comment(GeneratorSupport.defaultText(tableInfo.getComment(), "未填写表注释"))
                    .schema(GeneratorSupport.safeText(tableInfo.getSchema()))
                    .primaryKey(GeneratorSupport.safeText(tableInfo.getPrimaryKey()))
                    .engine(GeneratorSupport.safeText(tableInfo.getEngine()))
                    .charset(GeneratorSupport.safeText(tableInfo.getCharset()))
                    .collation(GeneratorSupport.safeText(tableInfo.getCollation()))
                    .rowFormat(GeneratorSupport.safeText(tableInfo.getRowFormat()))
                    .tableType(GeneratorSupport.safeText(tableInfo.getTableType()))
                    .columnCount(CollUtil.size(columns))
                    .showTableOverview(renderContext.hasSection("TABLE_OVERVIEW"))
                    .showBasicColumns(renderContext.hasSection("COLUMN_BASIC"))
                    .showExtendedColumns(renderContext.hasSection("COLUMN_EXTENDED"))
                    .showIndexes(renderContext.hasSection("INDEXES"))
                    .showForeignKeys(renderContext.hasSection("FOREIGN_KEYS"))
                    .hasBasicColumns(renderContext.hasSection("COLUMN_BASIC") && CollUtil.isNotEmpty(columns))
                    .hasExtendedColumns(renderContext.hasSection("COLUMN_EXTENDED") && CollUtil.isNotEmpty(columns))
                    .hasIndexes(renderContext.hasSection("INDEXES") && CollUtil.isNotEmpty(indexes))
                    .hasForeignKeys(renderContext.hasSection("FOREIGN_KEYS") && CollUtil.isNotEmpty(foreignKeys))
                    .columns(columns)
                    .extendedColumns(columns)
                    .indexes(indexes)
                    .foreignKeys(foreignKeys)
                    .basicColumnLayout(DocumentTableLayoutCalculator.buildBasicColumnLayout(columns))
                    .extendedColumnLayout(DocumentTableLayoutCalculator.buildExtendedColumnLayout(columns))
                    .indexLayout(DocumentTableLayoutCalculator.buildIndexLayout(indexes))
                    .foreignKeyLayout(DocumentTableLayoutCalculator.buildForeignKeyLayout(foreignKeys))
                    .build());
            tableNo++;
        }
        return tables;
    }

    private static List<DocumentColumnModel> buildColumns(List<ColumnInfo> columns, String booleanDisplayStyle) {
        if (CollUtil.isEmpty(columns)) {
            return List.of();
        }
        List<DocumentColumnModel> columnModels = CollUtil.newArrayList();
        int orderNo = 1;
        for (ColumnInfo columnInfo : columns) {
            String precisionScaleText = GeneratorSupport.buildPrecisionScaleText(columnInfo.getPrecision(), columnInfo.getScale());
            columnModels.add(DocumentColumnModel.builder()
                    .orderNo(columnInfo.getOrdinalPosition() != null ? columnInfo.getOrdinalPosition() : orderNo)
                    .name(GeneratorSupport.safeText(columnInfo.getName()))
                    .type(GeneratorSupport.safeText(columnInfo.getType()))
                    .primaryKeyText(GeneratorSupport.booleanDisplay(columnInfo.getPrimaryKey(), booleanDisplayStyle))
                    .nullableText(GeneratorSupport.booleanDisplay(columnInfo.getNullable(), booleanDisplayStyle))
                    .defaultValue(GeneratorSupport.safeText(columnInfo.getDefaultValue()))
                    .comment(GeneratorSupport.safeText(columnInfo.getComment()))
                    .rawType(GeneratorSupport.safeText(columnInfo.getRawType()))
                    .javaType(GeneratorSupport.safeText(columnInfo.getJavaType()))
                    .lengthText(GeneratorSupport.safeNumber(columnInfo.getLength()))
                    .precisionScaleText(precisionScaleText)
                    .autoIncrementText(GeneratorSupport.booleanDisplay(columnInfo.getAutoIncrement(), booleanDisplayStyle))
                    .generatedText(GeneratorSupport.booleanDisplay(columnInfo.getGenerated(), booleanDisplayStyle))
                    .extendedSummary(GeneratorSupport.buildExtendedSummary(columnInfo))
                    .build());
            orderNo++;
        }
        return columnModels;
    }

    private static List<DocumentIndexModel> buildIndexes(List<IndexInfo> indexes, String booleanDisplayStyle) {
        if (CollUtil.isEmpty(indexes)) {
            return List.of();
        }
        return indexes.stream()
                .map(indexInfo -> DocumentIndexModel.builder()
                        .name(GeneratorSupport.safeText(indexInfo.getName()))
                        .columnNamesText(GeneratorSupport.joinChinese(indexInfo.getColumnNames()))
                        .uniqueText(GeneratorSupport.booleanDisplay(indexInfo.getUnique(), booleanDisplayStyle))
                        .type(GeneratorSupport.safeText(indexInfo.getType()))
                        .build())
                .toList();
    }

    private static List<DocumentForeignKeyModel> buildForeignKeys(List<ForeignKeyInfo> foreignKeys) {
        if (CollUtil.isEmpty(foreignKeys)) {
            return List.of();
        }
        return foreignKeys.stream()
                .map(foreignKeyInfo -> DocumentForeignKeyModel.builder()
                        .name(GeneratorSupport.safeText(foreignKeyInfo.getName()))
                        .columnName(GeneratorSupport.safeText(foreignKeyInfo.getColumnName()))
                        .referencedTable(GeneratorSupport.safeText(foreignKeyInfo.getReferencedTable()))
                        .referencedColumn(GeneratorSupport.safeText(foreignKeyInfo.getReferencedColumn()))
                        .build())
                .toList();
    }
}
