package com.dbmetadoc.app.service;

import com.dbmetadoc.app.service.document.ExportSection;
import com.dbmetadoc.common.vo.DocumentOptionsResponse;
import com.dbmetadoc.common.vo.ExportSectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 文档导出选项服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Service
@RequiredArgsConstructor
public class DocumentOptionService {

    private final FontProfileService fontProfileService;

    public DocumentOptionsResponse loadOptions() {
        return DocumentOptionsResponse.builder()
                .defaultFontPreset(fontProfileService.defaultCode())
                .fontPresets(fontProfileService.listOptions())
                .exportSections(Arrays.stream(ExportSection.values())
                        .map(section -> ExportSectionResponse.builder()
                                .code(section.getCode())
                                .label(section.getLabel())
                                .description(section.getDescription())
                                .build())
                        .toList())
                .defaultExportSections(ExportSection.defaultCodes())
                .build();
    }
}


