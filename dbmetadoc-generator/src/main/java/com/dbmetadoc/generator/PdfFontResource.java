package com.dbmetadoc.generator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PDF 嵌入字体资源。
 *
 * @author mumu
 * @date 2026-04-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfFontResource {

    private String family;
    private String sourceUri;
}
