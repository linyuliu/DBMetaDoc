package com.dbmetadoc.app.controller;

import com.dbmetadoc.app.service.DocumentOptionService;
import com.dbmetadoc.app.service.DocumentService;
import com.dbmetadoc.app.service.GeneratedDocument;
import com.dbmetadoc.common.dto.DocumentCatalogRequest;
import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.response.R;
import com.dbmetadoc.common.vo.DocumentCatalogResponse;
import com.dbmetadoc.common.vo.DocumentOptionsResponse;
import com.dbmetadoc.common.vo.DocumentPreviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 文档接口控制器。
 *
 * @author mumu
 * @date 2026-03-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/document")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentOptionService documentOptionService;

    @GetMapping("/health")
    public R<String> health() {
        return R.ok("OK");
    }

    @GetMapping("/options")
    public R<DocumentOptionsResponse> options() {
        return R.ok(documentOptionService.loadOptions());
    }

    @PostMapping("/catalog")
    public CompletableFuture<R<DocumentCatalogResponse>> catalog(@Valid @RequestBody DocumentCatalogRequest request) {
        return documentService.catalogAsync(request).thenApply(R::ok);
    }

    @PostMapping("/preview")
    public CompletableFuture<R<DocumentPreviewResponse>> preview(@Valid @RequestBody DocumentRequest request) {
        return documentService.previewAsync(request).thenApply(R::ok);
    }

    @PostMapping("/export")
    public CompletableFuture<ResponseEntity<byte[]>> export(@Valid @RequestBody DocumentRequest request) {
        return documentService.exportAsync(request).thenApply(this::buildExportResponse);
    }

    private ResponseEntity<byte[]> buildExportResponse(GeneratedDocument generatedDocument) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(generatedDocument.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + generatedDocument.getFileName() + "\"")
                .body(generatedDocument.getContent());
    }
}


