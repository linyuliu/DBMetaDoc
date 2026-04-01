package com.dbmetadoc.generator;

import com.dbmetadoc.generator.model.DocumentColumnModel;
import com.dbmetadoc.generator.model.DocumentForeignKeyModel;
import com.dbmetadoc.generator.model.DocumentIndexModel;
import com.dbmetadoc.generator.model.DocumentTableColumnLayout;
import com.dbmetadoc.generator.model.DocumentTableLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTableLayoutCalculatorTest {

    @Test
    void shouldClampBasicColumnBudgetsAndKeepRatiosStable() {
        List<DocumentColumnModel> rows = List.of(
                DocumentColumnModel.builder()
                        .orderNo(1)
                        .name("id")
                        .type("BIGINT")
                        .primaryKeyText("是")
                        .nullableText("否")
                        .defaultValue("0")
                        .comment("主键")
                        .build(),
                DocumentColumnModel.builder()
                        .orderNo(2)
                        .name("extremely_long_order_identifier_name_for_rendering")
                        .type("VARCHAR(256)")
                        .primaryKeyText("否")
                        .nullableText("否")
                        .defaultValue("CURRENT_TIMESTAMP")
                        .comment("用于验证长英文列名和长默认值不会把短标记列挤坏")
                        .build()
        );

        DocumentTableLayout layout = DocumentTableLayoutCalculator.buildBasicColumnLayout(rows);

        assertEquals(7, layout.getColumns().size());
        assertBudgetBetween(layout.getColumns().get(0), 3, 4);
        assertBudgetBetween(layout.getColumns().get(1), 10, 18);
        assertBudgetBetween(layout.getColumns().get(2), 9, 15);
        assertBudgetBetween(layout.getColumns().get(3), 3, 4);
        assertBudgetBetween(layout.getColumns().get(4), 3, 4);
        assertBudgetBetween(layout.getColumns().get(5), 10, 16);
        assertBudgetBetween(layout.getColumns().get(6), 14, 34);
        assertTrue(layout.getColumns().get(6).getCharacterBudget() > layout.getColumns().get(4).getCharacterBudget());

        double totalRatio = layout.getColumns().stream()
                .mapToDouble(DocumentTableColumnLayout::getWidthRatio)
                .sum();
        assertTrue(totalRatio > 0.999d && totalRatio < 1.001d);
        assertEquals(1, layout.getRowLineCounts().get(0));
        assertTrue(layout.getRowLineCounts().get(1) >= 2);
    }

    @Test
    void shouldIncreaseRowLinesForLongIndexAndForeignKeyRows() {
        DocumentTableLayout indexLayout = DocumentTableLayoutCalculator.buildIndexLayout(List.of(
                DocumentIndexModel.builder()
                        .name("PRIMARY")
                        .columnNamesText("id")
                        .uniqueText("是")
                        .type("BTREE")
                        .build(),
                DocumentIndexModel.builder()
                        .name("idx_really_long_business_order_identifier_for_layout_validation")
                        .columnNamesText("business_order_identifier, tenant_identifier, external_reference_code")
                        .uniqueText("否")
                        .type("BTREE")
                        .build()
        ));
        DocumentTableLayout foreignKeyLayout = DocumentTableLayoutCalculator.buildForeignKeyLayout(List.of(
                DocumentForeignKeyModel.builder()
                        .name("fk_order_user")
                        .columnName("user_id")
                        .referencedTable("sys_user")
                        .referencedColumn("id")
                        .build(),
                DocumentForeignKeyModel.builder()
                        .name("fk_really_long_order_archive_relation_name_for_row_height")
                        .columnName("business_order_archive_identifier")
                        .referencedTable("biz_order_archive_history_snapshot")
                        .referencedColumn("archive_identifier")
                        .build()
        ));

        assertEquals(1, indexLayout.getRowLineCounts().get(0));
        assertTrue(indexLayout.getRowLineCounts().get(1) >= 2);
        assertEquals(1, foreignKeyLayout.getRowLineCounts().get(0));
        assertTrue(foreignKeyLayout.getRowLineCounts().get(1) >= 2);
        assertBudgetBetween(indexLayout.getColumns().get(2), 3, 4);
    }

    private void assertBudgetBetween(DocumentTableColumnLayout column, double min, double max) {
        assertTrue(column.getCharacterBudget() >= min);
        assertTrue(column.getCharacterBudget() <= max);
    }
}
