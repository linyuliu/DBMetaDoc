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
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DatasourceService datasourceService;
    private final TargetDatabaseService targetDatabaseService;
    private final MetadataCacheService metadataCacheService;

    public DocumentPreviewResponse preview(DocumentRequest request) {
        var connectionInfo = datasourceService.toConnectionInfo(request);
        DatabaseInfo databaseInfo = loadMetadata(request, connectionInfo);
        try {
            String html = new HtmlDocumentGenerator().generateHtml(databaseInfo, resolveTitle(request, connectionInfo));
            return DocumentPreviewResponse.builder()
                    .title(resolveTitle(request, connectionInfo))
                    .html(html)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "文档预览生成失败: " + e.getMessage(), e);
        }
    }

    public GeneratedDocument export(DocumentRequest request) {
        var connectionInfo = datasourceService.toConnectionInfo(request);
        DatabaseInfo databaseInfo = loadMetadata(request, connectionInfo);
        String format = request.getFormat() == null ? null : request.getFormat().toUpperCase();
        try {
            DocumentGenerator generator = DocumentGeneratorFactory.create(format);
            return GeneratedDocument.builder()
                    .fileName(resolveFileName(format))
                    .contentType(resolveContentType(format))
                    .content(generator.generate(databaseInfo, resolveTitle(request, connectionInfo)))
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "文档导出失败: " + e.getMessage(), e);
        }
    }

    private DatabaseInfo loadMetadata(DocumentRequest request, com.dbmetadoc.db.core.DatabaseConnectionInfo connectionInfo) {
        boolean useCache = !Boolean.FALSE.equals(request.getUseCache());
        boolean forceRefresh = Boolean.TRUE.equals(request.getForceRefresh());
        log.info("开始加载元数据，数据库类型：{}，主机：{}，数据库：{}，Schema：{}，缓存：{}，强制刷新：{}",
                connectionInfo.getType().name(), connectionInfo.getHost(), connectionInfo.getDatabase(),
                connectionInfo.getSchema(), useCache, forceRefresh);
        if (useCache && !forceRefresh) {
            var cached = metadataCacheService.find(connectionInfo);
            if (cached.isPresent()) {
                log.info("命中元数据缓存，数据库类型：{}，数据库：{}，Schema：{}",
                        connectionInfo.getType().name(), connectionInfo.getDatabase(), connectionInfo.getSchema());
                return cached.get();
            }
        }
        DatabaseInfo databaseInfo = targetDatabaseService.extract(connectionInfo);
        if (useCache) {
            metadataCacheService.save(connectionInfo, databaseInfo);
        }
        log.info("元数据加载完成，数据库类型：{}，表数量：{}",
                connectionInfo.getType().name(),
                databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size());
        return databaseInfo;
    }

    private String resolveTitle(DocumentRequest request, com.dbmetadoc.db.core.DatabaseConnectionInfo connectionInfo) {
        if (StrUtil.isNotBlank(request.getTitle())) {
            return request.getTitle();
        }
        return connectionInfo.getDatabase() + " 数据库文档";
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
