package com.dbmetadoc.generator;

import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.common.model.DatabaseInfo;
import com.dbmetadoc.common.model.TableInfo;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;

public class WordDocumentGenerator implements DocumentGenerator {

    @Override
    public String getFormat() {
        return "WORD";
    }

    @Override
    public byte[] generate(DatabaseInfo databaseInfo, String title) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            // Title
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title != null ? title : "Database Documentation");
            titleRun.setBold(true);
            titleRun.setFontSize(20);

            // Database info
            XWPFParagraph infoPara = document.createParagraph();
            XWPFRun infoRun = infoPara.createRun();
            infoRun.setText("Database: " + databaseInfo.getName()
                    + "  Type: " + databaseInfo.getType()
                    + (databaseInfo.getVersion() != null ? "  Version: " + databaseInfo.getVersion() : ""));

            if (databaseInfo.getTables() != null) {
                int tableIndex = 1;
                for (TableInfo table : databaseInfo.getTables()) {
                    // Table heading
                    XWPFParagraph heading = document.createParagraph();
                    heading.setStyle("Heading1");
                    XWPFRun headingRun = heading.createRun();
                    String headingText = tableIndex + ". " + table.getName();
                    if (table.getComment() != null && !table.getComment().isEmpty()) {
                        headingText += " - " + table.getComment();
                    }
                    headingRun.setText(headingText);

                    // Columns heading
                    XWPFParagraph colHeading = document.createParagraph();
                    colHeading.setStyle("Heading2");
                    colHeading.createRun().setText("Columns");

                    // Columns table
                    if (table.getColumns() != null && !table.getColumns().isEmpty()) {
                        XWPFTable colTable = document.createTable();
                        setTableWidth(colTable);

                        // Header row
                        XWPFTableRow headerRow = colTable.getRow(0);
                        setCell(headerRow, 0, "#");
                        addCell(headerRow, "Column Name");
                        addCell(headerRow, "Type");
                        addCell(headerRow, "Length");
                        addCell(headerRow, "Nullable");
                        addCell(headerRow, "Default");
                        addCell(headerRow, "Comment");
                        addCell(headerRow, "PK");

                        int colIdx = 1;
                        for (ColumnInfo col : table.getColumns()) {
                            XWPFTableRow row = colTable.createRow();
                            setCell(row, 0, String.valueOf(colIdx++));
                            setCell(row, 1, col.getName());
                            setCell(row, 2, col.getType() != null ? col.getType() : "");
                            setCell(row, 3, col.getLength() != null ? String.valueOf(col.getLength()) : "");
                            setCell(row, 4, col.isNullable() ? "YES" : "NO");
                            setCell(row, 5, col.getDefaultValue() != null ? col.getDefaultValue() : "");
                            setCell(row, 6, col.getComment() != null ? col.getComment() : "");
                            setCell(row, 7, col.isPrimaryKey() ? "YES" : "");
                        }
                    }

                    tableIndex++;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    private void setTableWidth(XWPFTable table) {
        CTTblWidth tblWidth = table.getCTTbl().getTblPr().addNewTblW();
        tblWidth.setType(STTblWidth.PCT);
        tblWidth.setW(BigInteger.valueOf(5000));
    }

    private void setCell(XWPFTableRow row, int cellIndex, String text) {
        XWPFTableCell cell = row.getCell(cellIndex);
        if (cell == null) {
            cell = row.addNewTableCell();
        }
        cell.setText(text != null ? text : "");
    }

    private void addCell(XWPFTableRow row, String text) {
        XWPFTableCell cell = row.addNewTableCell();
        cell.setText(text != null ? text : "");
    }
}
