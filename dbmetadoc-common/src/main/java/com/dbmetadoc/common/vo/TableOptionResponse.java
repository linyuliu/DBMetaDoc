package com.dbmetadoc.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表选择项响应。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableOptionResponse {

    private String key;
    private String name;
    private String schema;
    private String comment;
    private Integer columnCount;
}


