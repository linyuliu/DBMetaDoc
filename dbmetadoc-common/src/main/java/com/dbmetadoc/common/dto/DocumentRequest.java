package com.dbmetadoc.common.dto;

public class DocumentRequest {

    private String dbType;
    private String host;
    private Integer port;
    private String database;
    private String username;
    private String password;
    private String format;
    private String title;

    public DocumentRequest() {}

    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
