package com.dbmetadoc.app.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 生成文档结果对象。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedDocument {

    private String fileName;

    private String contentType;

    private byte[] content;
}
