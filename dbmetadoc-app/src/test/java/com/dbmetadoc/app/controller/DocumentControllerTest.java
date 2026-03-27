package com.dbmetadoc.app.controller;

import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.vo.DocumentPreviewResponse;
import com.dbmetadoc.app.service.DocumentService;
import com.dbmetadoc.app.service.GeneratedDocument;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest {

    private final DocumentService documentService = mock(DocumentService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new DocumentController(documentService))
            .setControllerAdvice(new com.dbmetadoc.app.exception.GlobalExceptionHandler())
            .build();

    @Test
    void shouldReturnWrappedPreviewResponse() throws Exception {
        when(documentService.preview(any(DocumentRequest.class)))
                .thenReturn(DocumentPreviewResponse.builder().title("demo").html("<h1>demo</h1>").build());

        mockMvc.perform(post("/api/document/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dbType":"MYSQL",
                                  "host":"127.0.0.1",
                                  "port":3306,
                                  "database":"demo",
                                  "username":"root",
                                  "password":"123456",
                                  "format":"HTML"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("demo"));
    }

    @Test
    void shouldReturnFileStreamOnExport() throws Exception {
        when(documentService.export(any(DocumentRequest.class)))
                .thenReturn(GeneratedDocument.builder()
                        .fileName("database-doc.html")
                        .contentType("text/html;charset=UTF-8")
                        .content("<h1>demo</h1>".getBytes())
                        .build());

        mockMvc.perform(post("/api/document/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dbType":"MYSQL",
                                  "host":"127.0.0.1",
                                  "port":3306,
                                  "database":"demo",
                                  "username":"root",
                                  "password":"123456",
                                  "format":"HTML"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"database-doc.html\""))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string("<h1>demo</h1>"));
    }
}
