package com.dbmetadoc.app.controller;

import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.response.R;
import com.dbmetadoc.common.vo.DocumentPreviewResponse;
import com.dbmetadoc.app.service.DocumentService;
import com.dbmetadoc.app.service.GeneratedDocument;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/document")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/health")
    public R<String> health() {
        return R.ok("OK");
    }

    @PostMapping("/preview")
    public R<DocumentPreviewResponse> preview(@Valid @RequestBody DocumentRequest request) {
        return R.ok(documentService.preview(request));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@Valid @RequestBody DocumentRequest request) {
        GeneratedDocument generatedDocument = documentService.export(request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(generatedDocument.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + generatedDocument.getFileName() + "\"")
                .body(generatedDocument.getContent());
    }
}
