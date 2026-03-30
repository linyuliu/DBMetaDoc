package com.dbmetadoc.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一文档模板视图模型。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplateModel {

    private String title;
    private String subtitle;
    private String generatedAt;
    private String databaseName;
    private String type;
    private String version;
    private String schemaName;
    private String catalogName;
    private String charset;
    private String collation;
    private Integer tableCount;
    private Boolean hasTables;
    private Boolean showDatabaseOverview;
    private Boolean showTableOverview;
    private Boolean showBasicColumns;
    private Boolean showExtendedColumns;
    private Boolean showIndexes;
    private Boolean showForeignKeys;
    private List<DocumentTableModel> tableOverviewRows;
    private List<DocumentTableModel> tables;
    private DocumentTheme theme;
}
