package com.dbmetadoc.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表格列布局信息。
 *
 * @author mumu
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTableColumnLayout {

    private String key;
    private String header;
    private Double characterBudget;
    private Double widthRatio;
    private String htmlWidthPercent;
}
