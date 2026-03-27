package com.dbmetadoc.common.model;

import java.util.List;

public class IndexInfo {

    private String name;
    private String tableName;
    private boolean unique;
    private List<String> columnNames;
    private String type;

    public IndexInfo() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public boolean isUnique() { return unique; }
    public void setUnique(boolean unique) { this.unique = unique; }

    public List<String> getColumnNames() { return columnNames; }
    public void setColumnNames(List<String> columnNames) { this.columnNames = columnNames; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
