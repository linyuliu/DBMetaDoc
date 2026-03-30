package com.dbmetadoc.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档导出目录响应。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCatalogResponse {

    private String databaseName;
    private String schemaName;
    private Integer tableCount;
    private List<TableOptionResponse> tables;
}


