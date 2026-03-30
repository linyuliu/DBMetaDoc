package com.dbmetadoc.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档字段视图模型。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentColumnModel {

    private Integer orderNo;
    private String name;
    private String type;
    private String primaryKeyText;
    private String nullableText;
    private String defaultValue;
    private String comment;
    private String rawType;
    private String javaType;
    private String lengthText;
    private String precisionScaleText;
    private String autoIncrementText;
    private String generatedText;
    private String extendedSummary;
}
