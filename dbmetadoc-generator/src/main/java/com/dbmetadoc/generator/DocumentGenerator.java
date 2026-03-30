package com.dbmetadoc.generator;


/**
 * 文档生成器接口。
 *
 * @author mumu
 * @date 2026-03-30
 */
public interface DocumentGenerator {

    byte[] generate(DocumentRenderContext renderContext) throws Exception;

    String getFormat();
}
