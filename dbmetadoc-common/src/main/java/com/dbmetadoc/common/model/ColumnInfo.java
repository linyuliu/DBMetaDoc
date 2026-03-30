package com.dbmetadoc.common.model;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 字段信息对象。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private Boolean nullable;
    private Boolean primaryKey;
    private Boolean autoIncrement;
    private Boolean generated;
    private String javaType;
    private String defaultValue;
    private String comment;
    private Integer ordinalPosition;
    private String rawType;
}
