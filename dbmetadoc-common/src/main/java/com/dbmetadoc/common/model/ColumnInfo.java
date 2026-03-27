package com.dbmetadoc.common.model;

public class ColumnInfo {

    private String name;
    private String type;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private boolean nullable;
    private boolean primaryKey;
    private String defaultValue;
    private String comment;
    private Integer ordinalPosition;

    public ColumnInfo() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }

    public Integer getPrecision() { return precision; }
    public void setPrecision(Integer precision) { this.precision = precision; }

    public Integer getScale() { return scale; }
    public void setScale(Integer scale) { this.scale = scale; }

    public boolean isNullable() { return nullable; }
    public void setNullable(boolean nullable) { this.nullable = nullable; }

    public boolean isPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Integer getOrdinalPosition() { return ordinalPosition; }
    public void setOrdinalPosition(Integer ordinalPosition) { this.ordinalPosition = ordinalPosition; }
}
