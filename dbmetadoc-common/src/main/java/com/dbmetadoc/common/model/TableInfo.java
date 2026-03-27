package com.dbmetadoc.common.model;

import java.util.List;

public class TableInfo {

    private String name;
    private String comment;
    private String schema;
    private List<ColumnInfo> columns;
    private List<IndexInfo> indexes;
    private List<ForeignKeyInfo> foreignKeys;

    public TableInfo() {}

    public TableInfo(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }

    public List<ColumnInfo> getColumns() { return columns; }
    public void setColumns(List<ColumnInfo> columns) { this.columns = columns; }

    public List<IndexInfo> getIndexes() { return indexes; }
    public void setIndexes(List<IndexInfo> indexes) { this.indexes = indexes; }

    public List<ForeignKeyInfo> getForeignKeys() { return foreignKeys; }
    public void setForeignKeys(List<ForeignKeyInfo> foreignKeys) { this.foreignKeys = foreignKeys; }
}
