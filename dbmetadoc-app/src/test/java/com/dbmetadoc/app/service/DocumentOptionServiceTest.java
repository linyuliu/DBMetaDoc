package com.dbmetadoc.app.service;

import com.dbmetadoc.app.properties.FontProfileProperties;
import com.dbmetadoc.common.vo.FontPresetResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentOptionServiceTest {

    @Test
    void shouldHideTableOverviewFromVisibleExportOptions() {
        FontProfileService fontProfileService = new FontProfileService(new FontProfileProperties()) {
            @Override
            public List<FontPresetResponse> listOptions() {
                return List.of();
            }

            @Override
            public String defaultCode() {
                return "modern-cn";
            }
        };
        DocumentOptionService service = new DocumentOptionService(fontProfileService);

        var response = service.loadOptions();

        assertTrue(response.getExportSections().stream().noneMatch(section -> "TABLE_OVERVIEW".equals(section.getCode())));
        assertFalse(response.getDefaultExportSections().contains("TABLE_OVERVIEW"));
    }
}
