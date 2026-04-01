package com.dbmetadoc.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表格布局信息。
 *
 * @author mumu
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTableLayout {

    private List<DocumentTableColumnLayout> columns;
    private List<Integer> rowLineCounts;
    private List<String> rowClasses;
}
