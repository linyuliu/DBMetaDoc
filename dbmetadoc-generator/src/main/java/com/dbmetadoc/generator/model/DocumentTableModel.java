package com.dbmetadoc.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档数据表视图模型。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTableModel {

    private Integer tableNo;
    private String chapterTitle;
    private String name;
    private String comment;
    private String schema;
    private String primaryKey;
    private String engine;
    private String charset;
    private String collation;
    private String rowFormat;
    private String tableType;
    private Integer columnCount;
    private Boolean showTableOverview;
    private Boolean showBasicColumns;
    private Boolean showExtendedColumns;
    private Boolean showIndexes;
    private Boolean showForeignKeys;
    private Boolean hasBasicColumns;
    private Boolean hasExtendedColumns;
    private Boolean hasIndexes;
    private Boolean hasForeignKeys;
    private List<DocumentColumnModel> columns;
    private List<DocumentColumnModel> extendedColumns;
    private List<DocumentIndexModel> indexes;
    private List<DocumentForeignKeyModel> foreignKeys;
}
