package com.dbmetadoc.app.service;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.app.service.document.ResolvedFontProfile;
import com.dbmetadoc.common.dto.DocumentCatalogRequest;
import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.vo.DocumentCatalogResponse;
import com.dbmetadoc.common.vo.DocumentPreviewResponse;
import com.dbmetadoc.generator.DocumentGenerator;
import com.dbmetadoc.generator.DocumentGeneratorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * 文档预览、目录和导出服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private static final String DEFAULT_TITLE_SUFFIX = " 数据库文档";
    private static final String DEFAULT_EXPORT_FILE_PREFIX = "database-doc";
    private static final String HTML_FORMAT = "HTML";
    private static final String PDF_FORMAT = "PDF";
    private static final String WORD_FORMAT = "WORD";
    private static final String DOC_FORMAT = "DOC";
    private static final String DOCX_FORMAT = "DOCX";
    private static final String EXCEL_FORMAT = "EXCEL";
    private static final String XLS_FORMAT = "XLS";
    private static final String XLSX_FORMAT = "XLSX";
    private static final String MARKDOWN_FORMAT = "MARKDOWN";
    private static final String MD_FORMAT = "MD";

    private final DatasourceService datasourceService;
    private final TargetDatabaseService targetDatabaseService;
    private final MetadataCacheService metadataCacheService;
    private final DocumentFilterService documentFilterService;
    private final FontProfileService fontProfileService;
    private final TaskExecutor taskExecutor;

    public DocumentCatalogResponse catalog(DocumentCatalogRequest request) {
        return catalogAsync(request).join();
    }

    public CompletableFuture<DocumentCatalogResponse> catalogAsync(DocumentCatalogRequest request) {
        var connectionInfo = datasourceService.toConnectionInfo(request);
        return loadMetadataAsync(request.getDatasourceId(), request.getUseCache(), request.getForceRefresh(), connectionInfo)
                .thenApplyAsync(documentFilterService::buildCatalog, taskExecutor);
    }

    public DocumentPreviewResponse preview(DocumentRequest request) {
        return previewAsync(request).join();
    }

    public CompletableFuture<DocumentPreviewResponse> previewAsync(DocumentRequest request) {
        var connectionInfo = datasourceService.toConnectionInfo(request);
        String title = resolveTitle(request, connectionInfo);
        return buildRenderContextAsync(request, connectionInfo, title)
                .thenApplyAsync(renderContext -> buildPreviewResponse(renderContext, title), taskExecutor);
    }

    public GeneratedDocument export(DocumentRequest request) {
        return exportAsync(request).join();
    }

    public CompletableFuture<GeneratedDocument> exportAsync(DocumentRequest request) {
        var connectionInfo = datasourceService.toConnectionInfo(request);
        String format = normalizeFormat(request.getFormat());
        String title = resolveTitle(request, connectionInfo);
        return buildRenderContextAsync(request, connectionInfo, title)
                .thenApplyAsync(renderContext -> buildGeneratedDocument(renderContext, format), taskExecutor);
    }

    private CompletableFuture<com.dbmetadoc.generator.DocumentRenderContext> buildRenderContextAsync(
            DocumentRequest request,
            com.dbmetadoc.db.core.DatabaseConnectionInfo connectionInfo,
            String title) {
        return loadMetadataAsync(request.getDatasourceId(), request.getUseCache(), request.getForceRefresh(), connectionInfo)
                .thenApplyAsync(databaseInfo -> {
                    ResolvedFontProfile fontProfile = fontProfileService.resolve(request.getFontPreset());
                    return documentFilterService.buildRenderContext(databaseInfo, request, title, fontProfile);
                }, taskExecutor);
    }

    private CompletableFuture<DatabaseInfo> loadMetadataAsync(Long datasourceId,
                                                              Boolean useCacheFlag,
                                                              Boolean forceRefreshFlag,
                                                              com.dbmetadoc.db.core.DatabaseConnectionInfo connectionInfo) {
        boolean useCache = !Boolean.FALSE.equals(useCacheFlag);
        boolean forceRefresh = Boolean.TRUE.equals(forceRefreshFlag);
        log.info("开始加载元数据，数据库类型：{}，主机：{}，数据库：{}，Schema：{}，缓存：{}，强制刷新：{}",
                connectionInfo.getType().name(),
                connectionInfo.getHost(),
                connectionInfo.getDatabase(),
                connectionInfo.getSchema(),
                useCache,
                forceRefresh);
        if (useCache && !forceRefresh) {
            var cached = metadataCacheService.find(connectionInfo);
            if (cached.isPresent()) {
                touchDatasource(datasourceId);
                log.info("命中元数据缓存，数据库类型：{}，数据库：{}，Schema：{}",
                        connectionInfo.getType().name(), connectionInfo.getDatabase(), connectionInfo.getSchema());
                return CompletableFuture.completedFuture(cached.get());
            }
        }
        return targetDatabaseService.extractAsync(connectionInfo)
                .thenApply(databaseInfo -> {
                    if (useCache) {
                        metadataCacheService.save(connectionInfo, databaseInfo);
                    }
                    touchDatasource(datasourceId);
                    log.info("元数据加载完成，数据库类型：{}，表数量：{}",
                            connectionInfo.getType().name(),
                            databaseInfo.getTables() == null ? 0 : databaseInfo.getTables().size());
                    return databaseInfo;
                });
    }

    private String resolveTitle(DocumentRequest request, com.dbmetadoc.db.core.DatabaseConnectionInfo connectionInfo) {
        if (StrUtil.isNotBlank(request.getTitle())) {
            return request.getTitle();
        }
        return connectionInfo.getDatabase() + DEFAULT_TITLE_SUFFIX;
    }

    private DocumentPreviewResponse buildPreviewResponse(com.dbmetadoc.generator.DocumentRenderContext renderContext, String title) {
        try {
            DocumentGenerator generator = DocumentGeneratorFactory.create(HTML_FORMAT);
            return DocumentPreviewResponse.builder()
                    .title(title)
                    .html(new String(generator.generate(renderContext), StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "文档预览生成失败: " + e.getMessage(), e);
        }
    }

    private GeneratedDocument buildGeneratedDocument(com.dbmetadoc.generator.DocumentRenderContext renderContext, String format) {
        try {
            DocumentGenerator generator = DocumentGeneratorFactory.create(format);
            return GeneratedDocument.builder()
                    .fileName(resolveFileName(format))
                    .contentType(resolveContentType(format))
                    .content(generator.generate(renderContext))
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DOCUMENT_GENERATE_FAILED, "文档导出失败: " + e.getMessage(), e);
        }
    }

    private String normalizeFormat(String format) {
        return StrUtil.blankToDefault(format, HTML_FORMAT).toUpperCase(Locale.ROOT);
    }

    private String resolveContentType(String format) {
        return switch (normalizeFormat(format)) {
            case PDF_FORMAT -> "application/pdf";
            case WORD_FORMAT, DOC_FORMAT, DOCX_FORMAT -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case EXCEL_FORMAT, XLS_FORMAT, XLSX_FORMAT -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case MARKDOWN_FORMAT, MD_FORMAT -> "text/markdown;charset=UTF-8";
            default -> "text/html;charset=UTF-8";
        };
    }

    private String resolveFileName(String format) {
        return switch (normalizeFormat(format)) {
            case PDF_FORMAT -> DEFAULT_EXPORT_FILE_PREFIX + ".pdf";
            case WORD_FORMAT, DOC_FORMAT, DOCX_FORMAT -> DEFAULT_EXPORT_FILE_PREFIX + ".docx";
            case EXCEL_FORMAT, XLS_FORMAT, XLSX_FORMAT -> DEFAULT_EXPORT_FILE_PREFIX + ".xlsx";
            case MARKDOWN_FORMAT, MD_FORMAT -> DEFAULT_EXPORT_FILE_PREFIX + ".md";
            default -> DEFAULT_EXPORT_FILE_PREFIX + ".html";
        };
    }

    private void touchDatasource(Long datasourceId) {
        if (datasourceId != null) {
            datasourceService.touch(datasourceId);
        }
    }
}


