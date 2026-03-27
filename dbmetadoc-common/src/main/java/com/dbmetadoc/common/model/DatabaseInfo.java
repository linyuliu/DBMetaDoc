package com.dbmetadoc.common.model;

import java.util.List;

public class DatabaseInfo {

    private String name;
    private String type;
    private String version;
    private String charset;
    private List<TableInfo> tables;

    public DatabaseInfo() {}

    public DatabaseInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }

    public List<TableInfo> getTables() { return tables; }
    public void setTables(List<TableInfo> tables) { this.tables = tables; }
}
