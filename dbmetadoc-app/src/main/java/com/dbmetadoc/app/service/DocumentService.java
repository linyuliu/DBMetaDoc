package com.dbmetadoc.app.service;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.vo.DocumentPreviewResponse;
import com.dbmetadoc.generator.DocumentGenerator;
import com.dbmetadoc.generator.DocumentGeneratorFactory;
import com.dbmetadoc.generator.HtmlDocumentGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DatasourceService datasourceService;
    private final TargetDatabaseService targetDatabaseService;
    private final MetadataCacheService metadataCacheService;

    public DocumentPreviewResponse preview(DocumentRequest request) {
        DatabaseInfo databaseInfo = loadMetadata(request);
        try {
            String html = new HtmlDocumentGenerator().generateHtml(databaseInfo, resolveTitle(request));
            return DocumentPreviewResponse.builder()
                    .title(resolveTitle(request))
                    .html(html)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "文档预览生成失败: " + e.getMessage(), e);
        }
    }

    public GeneratedDocument export(DocumentRequest request) {
        DatabaseInfo databaseInfo = loadMetadata(request);
        String format = request.getFormat() == null ? null : request.getFormat().toUpperCase();
        try {
            DocumentGenerator generator = DocumentGeneratorFactory.create(format);
            return GeneratedDocument.builder()
                    .fileName(resolveFileName(format))
                    .contentType(resolveContentType(format))
                    .content(generator.generate(databaseInfo, resolveTitle(request)))
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "文档导出失败: " + e.getMessage(), e);
        }
    }

    private DatabaseInfo loadMetadata(DocumentRequest request) {
        boolean useCache = !Boolean.FALSE.equals(request.getUseCache());
        boolean forceRefresh = Boolean.TRUE.equals(request.getForceRefresh());
        var connectionInfo = datasourceService.toConnectionInfo(request);
        if (useCache && !forceRefresh) {
            var cached = metadataCacheService.find(connectionInfo);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        DatabaseInfo databaseInfo = targetDatabaseService.extract(connectionInfo);
        if (useCache) {
            metadataCacheService.save(connectionInfo, databaseInfo);
        }
        return databaseInfo;
    }

    private String resolveTitle(DocumentRequest request) {
        if (StrUtil.isNotBlank(request.getTitle())) {
            return request.getTitle();
        }
        return request.getDatabase() + " 数据库文档";
    }

    private String resolveContentType(String format) {
        return switch (format) {
            case "PDF" -> "application/pdf";
            case "WORD", "DOC", "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "MARKDOWN", "MD" -> "text/markdown;charset=UTF-8";
            default -> "text/html;charset=UTF-8";
        };
    }

    private String resolveFileName(String format) {
        return switch (format) {
            case "PDF" -> "database-doc.pdf";
            case "WORD", "DOC", "DOCX" -> "database-doc.docx";
            case "MARKDOWN", "MD" -> "database-doc.md";
            default -> "database-doc.html";
        };
    }
}
