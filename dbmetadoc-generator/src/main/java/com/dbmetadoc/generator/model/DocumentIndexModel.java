package com.dbmetadoc.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档索引视图模型。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIndexModel {

    private String name;
    private String columnNamesText;
    private String uniqueText;
    private String type;
}
