package com.dbmetadoc.app.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.app.service.document.ExportSection;
import com.dbmetadoc.app.service.document.ResolvedFontProfile;
import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;
import com.dbmetadoc.common.vo.DocumentCatalogResponse;
import com.dbmetadoc.common.vo.TableOptionResponse;
import com.dbmetadoc.generator.DocumentRenderContext;
import com.dbmetadoc.generator.FontRenderProfile;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档导出筛选服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Service
public class DocumentFilterService {

    public DocumentCatalogResponse buildCatalog(DatabaseInfo databaseInfo) {
        List<TableOptionResponse> tables = CollUtil.emptyIfNull(databaseInfo.getTables()).stream()
                .map(table -> TableOptionResponse.builder()
                        .key(buildTableKey(table))
                        .name(table.getName())
                        .schema(table.getSchema())
                        .comment(table.getComment())
                        .columnCount(CollUtil.size(table.getColumns()))
                        .build())
                .toList();
        return DocumentCatalogResponse.builder()
                .databaseName(StrUtil.blankToDefault(databaseInfo.getDatabaseName(), databaseInfo.getName()))
                .schemaName(databaseInfo.getSchemaName())
                .tableCount(tables.size())
                .tables(tables)
                .build();
    }

    public DocumentRenderContext buildRenderContext(DatabaseInfo databaseInfo,
                                                    DocumentRequest request,
                                                    String title,
                                                    ResolvedFontProfile fontProfile) {
        EnumSet<ExportSection> sections = ExportSection.fromCodes(request.getExportSections());
        DatabaseInfo filteredDatabase = filterDatabase(databaseInfo, sections, request.getSelectedTableKeys());
        return DocumentRenderContext.builder()
                .title(title)
                .database(filteredDatabase)
                .visibleSections(ExportSection.codesOf(sections))
                .fontProfile(toFontRenderProfile(fontProfile))
                .booleanDisplayStyle(request.getBooleanDisplayStyle())
                .build();
    }

    private DatabaseInfo filterDatabase(DatabaseInfo source, EnumSet<ExportSection> sections, List<String> selectedTableKeys) {
        Set<String> tableKeySet = normalizeSelectedTableKeys(source, selectedTableKeys);
        boolean databaseOverview = sections.contains(ExportSection.DATABASE_OVERVIEW);
        List<TableInfo> filteredTables = CollUtil.emptyIfNull(source.getTables()).stream()
                .filter(table -> tableKeySet.contains(buildTableKey(table)))
                .map(table -> filterTable(table, sections))
                .toList();
        return DatabaseInfo.builder()
                .name(source.getName())
                .type(databaseOverview ? source.getType() : null)
                .version(databaseOverview ? source.getVersion() : null)
                .driverName(databaseOverview ? source.getDriverName() : null)
                .databaseName(databaseOverview ? source.getDatabaseName() : null)
                .schemaName(databaseOverview ? source.getSchemaName() : null)
                .catalogName(databaseOverview ? source.getCatalogName() : null)
                .charset(databaseOverview ? source.getCharset() : null)
                .collation(databaseOverview ? source.getCollation() : null)
                .tables(filteredTables)
                .build();
    }

    private TableInfo filterTable(TableInfo table, EnumSet<ExportSection> sections) {
        boolean tableOverview = sections.contains(ExportSection.TABLE_OVERVIEW);
        boolean hasColumns = sections.contains(ExportSection.COLUMN_BASIC) || sections.contains(ExportSection.COLUMN_EXTENDED);
        return TableInfo.builder()
                .name(table.getName())
                .comment(table.getComment())
                .schema(table.getSchema())
                .primaryKey(table.getPrimaryKey())
                .tableType(table.getTableType())
                .engine(tableOverview ? table.getEngine() : null)
                .charset(tableOverview ? table.getCharset() : null)
                .collation(tableOverview ? table.getCollation() : null)
                .rowFormat(tableOverview ? table.getRowFormat() : null)
                .createOptions(tableOverview ? table.getCreateOptions() : null)
                .columns(hasColumns ? filterColumns(table.getColumns(), sections) : List.of())
                .indexes(sections.contains(ExportSection.INDEXES) ? safeIndexes(table.getIndexes()) : List.of())
                .foreignKeys(sections.contains(ExportSection.FOREIGN_KEYS) ? safeForeignKeys(table.getForeignKeys()) : List.of())
                .build();
    }

    private List<ColumnInfo> filterColumns(List<ColumnInfo> columns, EnumSet<ExportSection> sections) {
        boolean extended = sections.contains(ExportSection.COLUMN_EXTENDED);
        if (CollUtil.isEmpty(columns)) {
            return List.of();
        }
        return columns.stream()
                .map(column -> ColumnInfo.builder()
                        .name(column.getName())
                        .type(column.getType())
                        .length(extended ? column.getLength() : null)
                        .precision(extended ? column.getPrecision() : null)
                        .scale(extended ? column.getScale() : null)
                        .nullable(column.getNullable())
                        .primaryKey(column.getPrimaryKey())
                        .autoIncrement(extended ? column.getAutoIncrement() : null)
                        .generated(extended ? column.getGenerated() : null)
                        .javaType(extended ? column.getJavaType() : null)
                        .defaultValue(column.getDefaultValue())
                        .comment(column.getComment())
                        .ordinalPosition(extended ? column.getOrdinalPosition() : null)
                        .rawType(extended ? column.getRawType() : null)
                        .build())
                .toList();
    }

    private List<IndexInfo> safeIndexes(List<IndexInfo> indexes) {
        return CollUtil.emptyIfNull(indexes);
    }

    private List<ForeignKeyInfo> safeForeignKeys(List<ForeignKeyInfo> foreignKeys) {
        return CollUtil.emptyIfNull(foreignKeys);
    }

    private Set<String> normalizeSelectedTableKeys(DatabaseInfo databaseInfo, List<String> selectedTableKeys) {
        if (CollUtil.isNotEmpty(selectedTableKeys)) {
            return new LinkedHashSet<>(selectedTableKeys);
        }
        if (CollUtil.isEmpty(databaseInfo.getTables())) {
            return Set.of();
        }
        return databaseInfo.getTables().stream()
                .map(this::buildTableKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private FontRenderProfile toFontRenderProfile(ResolvedFontProfile fontProfile) {
        return FontRenderProfile.builder()
                .code(fontProfile.getCode())
                .label(fontProfile.getLabel())
                .titleFont(fontProfile.getTitleFont())
                .bodyFont(fontProfile.getBodyFont())
                .monoFont(fontProfile.getMonoFont())
                .symbolFont(fontProfile.getSymbolFont())
                .titleFontCss(fontProfile.getTitleFontCss())
                .bodyFontCss(fontProfile.getBodyFontCss())
                .monoFontCss(fontProfile.getMonoFontCss())
                .symbolFontCss(fontProfile.getSymbolFontCss())
                .pdfTitleFontCss(fontProfile.getPdfTitleFontCss())
                .pdfBodyFontCss(fontProfile.getPdfBodyFontCss())
                .pdfMonoFontCss(fontProfile.getPdfMonoFontCss())
                .pdfSymbolFontCss(fontProfile.getPdfSymbolFontCss())
                .pdfFontFiles(fontProfile.getPdfFontFiles())
                .pdfFontResources(fontProfile.getPdfFontResources())
                .build();
    }

    public String buildTableKey(TableInfo table) {
        if (StrUtil.isNotBlank(table.getSchema())) {
            return table.getSchema() + "." + table.getName();
        }
        return table.getName();
    }
}


