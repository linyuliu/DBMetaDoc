package com.dbmetadoc.app.service;

import com.dbmetadoc.app.service.document.ResolvedFontProfile;
import com.dbmetadoc.app.service.document.ExportSection;
import com.dbmetadoc.common.dto.DocumentRequest;
import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.ForeignKeyInfo;
import com.dbmetadoc.common.model.IndexInfo;
import com.dbmetadoc.common.model.TableInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentFilterServiceTest {

    private final DocumentFilterService documentFilterService = new DocumentFilterService();

    @Test
    void shouldFilterTablesAndSectionsForRenderContext() {
        DocumentRequest request = new DocumentRequest();
        request.setSelectedTableKeys(List.of("biz.order"));
        request.setExportSections(List.of("TABLE_OVERVIEW", "COLUMN_BASIC", "INDEXES"));

        var renderContext = documentFilterService.buildRenderContext(
                buildDatabaseInfo(),
                request,
                "订单文档",
                ResolvedFontProfile.builder()
                        .code("modern-cn")
                        .label("现代中文")
                        .titleFont("Microsoft YaHei")
                        .bodyFont("DengXian")
                        .monoFont("Cascadia Mono")
                        .titleFontCss("\"Microsoft YaHei\", sans-serif")
                        .bodyFontCss("\"DengXian\", sans-serif")
                        .monoFontCss("\"Cascadia Mono\", monospace")
                        .pdfFontFiles(List.of())
                        .build());

        assertEquals("订单文档", renderContext.getTitle());
        assertEquals(1, renderContext.getDatabase().getTables().size());
        assertEquals("order", renderContext.getDatabase().getTables().get(0).getName());
        assertNull(renderContext.getDatabase().getType());
        assertEquals(1, renderContext.getDatabase().getTables().get(0).getColumns().size());
        assertEquals("id", renderContext.getDatabase().getTables().get(0).getColumns().get(0).getName());
        assertEquals("0", renderContext.getDatabase().getTables().get(0).getColumns().get(0).getDefaultValue());
        assertEquals(1, renderContext.getDatabase().getTables().get(0).getIndexes().size());
        assertTrue(renderContext.getDatabase().getTables().get(0).getForeignKeys().isEmpty());
    }

    @Test
    void shouldExcludeColumnExtendedFromDefaultSections() {
        assertFalse(ExportSection.defaultCodes().contains("COLUMN_EXTENDED"));
    }

    private DatabaseInfo buildDatabaseInfo() {
        TableInfo orderTable = TableInfo.builder()
                .name("order")
                .schema("biz")
                .comment("订单表")
                .engine("InnoDB")
                .charset("utf8mb4")
                .collation("utf8mb4_general_ci")
                .columns(List.of(ColumnInfo.builder()
                        .name("id")
                        .type("BIGINT")
                        .comment("主键")
                        .primaryKey(true)
                        .nullable(false)
                        .defaultValue("0")
                        .build()))
                .indexes(List.of(IndexInfo.builder()
                        .name("idx_order_id")
                        .columnNames(List.of("id"))
                        .unique(true)
                        .type("BTREE")
                        .build()))
                .foreignKeys(List.of(ForeignKeyInfo.builder()
                        .name("fk_order_user")
                        .columnName("user_id")
                        .referencedTable("sys_user")
                        .referencedColumn("id")
                        .build()))
                .build();
        TableInfo userTable = TableInfo.builder()
                .name("sys_user")
                .schema("biz")
                .comment("用户表")
                .columns(List.of())
                .indexes(List.of())
                .foreignKeys(List.of())
                .build();
        return DatabaseInfo.builder()
                .name("demo")
                .type("MYSQL")
                .databaseName("demo")
                .schemaName("biz")
                .tables(List.of(orderTable, userTable))
                .build();
    }
}
