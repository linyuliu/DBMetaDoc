package com.dbmetadoc.web.controller;

import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.db.ConnectionFactory;
import com.dbmetadoc.db.DbType;
import com.dbmetadoc.db.MetadataExtractor;
import com.dbmetadoc.db.MetadataExtractorFactory;
import com.dbmetadoc.generator.DocumentGenerator;
import com.dbmetadoc.generator.DocumentGeneratorFactory;
import com.dbmetadoc.generator.HtmlDocumentGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.util.Map;

@RestController
@RequestMapping("/api/document")
// NOTE: Restrict origins in production via spring.web.cors configuration or a WebMvcConfigurer
@CrossOrigin(origins = "*")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@RequestBody DocumentRequest request) {
        try {
            DatabaseInfo databaseInfo = extractMetadata(request);

            String format = request.getFormat() != null ? request.getFormat().toUpperCase() : "HTML";
            DocumentGenerator generator = DocumentGeneratorFactory.create(format);
            byte[] content = generator.generate(databaseInfo, request.getTitle());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(resolveMediaType(format));
            headers.setContentDispositionFormData("attachment", resolveFileName(format));

            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Invalid request: " + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error generating document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Error generating document: " + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<String> preview(@RequestBody DocumentRequest request) {
        try {
            DatabaseInfo databaseInfo = extractMetadata(request);

            HtmlDocumentGenerator htmlGenerator = new HtmlDocumentGenerator();
            String html = htmlGenerator.generateHtml(databaseInfo, request.getTitle());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating preview: " + e.getMessage());
        }
    }

    private DatabaseInfo extractMetadata(DocumentRequest request) throws Exception {
        DbType dbType = DbType.valueOf(request.getDbType().toUpperCase());
        int port = request.getPort() != null ? request.getPort() : defaultPort(dbType);

        try (Connection connection = ConnectionFactory.create(
                dbType,
                request.getHost(),
                port,
                request.getDatabase(),
                request.getUsername(),
                request.getPassword())) {

            MetadataExtractor extractor = MetadataExtractorFactory.create(dbType);
            return extractor.extract(connection, request.getDatabase());
        }
    }

    private int defaultPort(DbType dbType) {
        switch (dbType) {
            case MYSQL: return 3306;
            case POSTGRESQL: return 5432;
            case KINGBASE: return 54321;
            default: return 3306;
        }
    }

    private MediaType resolveMediaType(String format) {
        switch (format) {
            case "PDF": return MediaType.APPLICATION_PDF;
            case "WORD":
            case "DOC":
            case "DOCX":
                return MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "MARKDOWN":
            case "MD":
                return MediaType.parseMediaType("text/markdown;charset=UTF-8");
            default:
                return MediaType.TEXT_HTML;
        }
    }

    private String resolveFileName(String format) {
        switch (format) {
            case "PDF": return "database-doc.pdf";
            case "WORD":
            case "DOC":
            case "DOCX": return "database-doc.docx";
            case "MARKDOWN":
            case "MD": return "database-doc.md";
            default: return "database-doc.html";
        }
    }
}
