package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Exports selected and displayed RMA records to formatted Excel workbooks. */
public class ExcelExportService {

    private static final DateTimeFormatter GENERATED_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");

    private static final String[] SELECTED_RMA_HEADERS = {
            "County",
            "Serial Number",
            "Machine Type",
            "Version",
            "Received"
    };

    private static final String[] DISPLAYED_RMA_HEADERS = {
            "RMA Number",
            "County",
            "Serial Number",
            "Machine Type",
            "Version",
            "Received"
    };

    public void exportSelectedRma(
            RmaRecord record,
            Path outputFile
    ) throws IOException {
        if (record == null) {
            throw new IllegalArgumentException("RMA record cannot be null.");
        }
        validateOutputFile(outputFile);

        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(outputFile)) {

            Sheet sheet = workbook.createSheet("RMA Machines");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtitleStyle = createSubtitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centeredDataStyle = createCenteredDataStyle(workbook);

            createMergedTitle(
                    sheet,
                    "RMA Number: " + valueOrBlank(record.getRmaNumber()),
                    SELECTED_RMA_HEADERS.length,
                    titleStyle
            );
            createGeneratedRow(sheet, SELECTED_RMA_HEADERS.length, subtitleStyle);
            createHeaderRow(sheet, SELECTED_RMA_HEADERS, headerStyle);

            int rowNumber = 3;
            List<RepairItem> repairItems = record.getRepairItems();

            if (repairItems == null || repairItems.isEmpty()) {
                createNoMachinesRow(
                        sheet,
                        rowNumber,
                        SELECTED_RMA_HEADERS.length,
                        dataStyle
                );
            } else {
                for (RepairItem item : repairItems) {
                    if (item == null) {
                        continue;
                    }

                    Row row = sheet.createRow(rowNumber++);
                    row.setHeightInPoints(21);
                    createCell(row, 0, item.getCounty(), dataStyle);
                    createCell(row, 1, item.getSerialNumber(), dataStyle);
                    createCell(row, 2, item.getMachineType(), dataStyle);
                    createCell(row, 3, item.getVersion(), dataStyle);
                    createCell(
                            row,
                            4,
                            item.isReceived() ? "X" : "",
                            centeredDataStyle
                    );
                }
            }

            configureSheet(
                    sheet,
                    SELECTED_RMA_HEADERS.length,
                    new int[]{18, 18, 18, 12, 10}
            );
            workbook.write(outputStream);
        }
    }

    /**
     * Exports every RMA currently displayed in the main table.
     * The supplied list should be the already-filtered display list.
     */
    public void exportDisplayedRmas(
            List<RmaRecord> records,
            Path outputFile
    ) throws IOException {
        if (records == null) {
            throw new IllegalArgumentException("Displayed RMA list cannot be null.");
        }
        validateOutputFile(outputFile);

        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(outputFile)) {

            Sheet sheet = workbook.createSheet("Displayed RMAs");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtitleStyle = createSubtitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centeredDataStyle = createCenteredDataStyle(workbook);

            createMergedTitle(
                    sheet,
                    "Displayed RMA Machines",
                    DISPLAYED_RMA_HEADERS.length,
                    titleStyle
            );
            createGeneratedRow(sheet, DISPLAYED_RMA_HEADERS.length, subtitleStyle);
            createHeaderRow(sheet, DISPLAYED_RMA_HEADERS, headerStyle);

            int rowNumber = 3;
            for (RmaRecord record : records) {
                if (record == null) {
                    continue;
                }

                List<RepairItem> repairItems = record.getRepairItems();
                if (repairItems == null || repairItems.isEmpty()) {
                    Row row = sheet.createRow(rowNumber++);
                    createCell(row, 0, record.getRmaNumber(), dataStyle);
                    createCell(row, 1, "", dataStyle);
                    createCell(row, 2, "", dataStyle);
                    createCell(row, 3, "", dataStyle);
                    createCell(row, 4, "", dataStyle);
                    createCell(row, 5, "", centeredDataStyle);
                    continue;
                }

                for (RepairItem item : repairItems) {
                    if (item == null) {
                        continue;
                    }

                    Row row = sheet.createRow(rowNumber++);
                    row.setHeightInPoints(21);
                    createCell(row, 0, record.getRmaNumber(), dataStyle);
                    createCell(row, 1, item.getCounty(), dataStyle);
                    createCell(row, 2, item.getSerialNumber(), dataStyle);
                    createCell(row, 3, item.getMachineType(), dataStyle);
                    createCell(row, 4, item.getVersion(), dataStyle);
                    createCell(
                            row,
                            5,
                            item.isReceived() ? "X" : "",
                            centeredDataStyle
                    );
                }
            }

            configureSheet(
                    sheet,
                    DISPLAYED_RMA_HEADERS.length,
                    new int[]{16, 18, 18, 18, 12, 10}
            );
            workbook.write(outputStream);
        }
    }

    private void validateOutputFile(Path outputFile) throws IOException {
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null.");
        }

        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void createMergedTitle(
            Sheet sheet,
            String title,
            int columnCount,
            CellStyle style
    ) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(30);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));
    }

    private void createGeneratedRow(
            Sheet sheet,
            int columnCount,
            CellStyle style
    ) {
        Row row = sheet.createRow(1);
        Cell cell = row.createCell(0);
        cell.setCellValue(
                "Generated: " + LocalDateTime.now().format(GENERATED_TIME_FORMAT)
        );
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnCount - 1));
    }

    private void createHeaderRow(
            Sheet sheet,
            String[] headers,
            CellStyle style
    ) {
        Row row = sheet.createRow(2);
        row.setHeightInPoints(24);

        for (int column = 0; column < headers.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(style);
        }
    }

    private void createNoMachinesRow(
            Sheet sheet,
            int rowNumber,
            int columnCount,
            CellStyle style
    ) {
        Row row = sheet.createRow(rowNumber);
        Cell cell = row.createCell(0);
        cell.setCellValue("No machines are attached to this RMA.");
        cell.setCellStyle(style);
        sheet.addMergedRegion(
                new CellRangeAddress(rowNumber, rowNumber, 0, columnCount - 1)
        );
    }

    private void createCell(
            Row row,
            int column,
            Object value,
            CellStyle style
    ) {
        Cell cell = row.createCell(column);
        cell.setCellValue(valueOrBlank(value));
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        addBorders(style);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        addBorders(style);
        return style;
    }

    private CellStyle createCenteredDataStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void addBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void configureSheet(
            Sheet sheet,
            int columnCount,
            int[] minimumCharacterWidths
    ) {
        sheet.createFreezePane(0, 3);
        sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, columnCount - 1));

        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            int minimumWidth = minimumCharacterWidths[column] * 256;
            int paddedWidth = sheet.getColumnWidth(column) + (2 * 256);
            int finalWidth = Math.max(minimumWidth, paddedWidth);
            sheet.setColumnWidth(column, Math.min(finalWidth, 60 * 256));
        }

        sheet.setFitToPage(true);
        sheet.getPrintSetup().setLandscape(columnCount > 5);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setHorizontallyCenter(true);
        sheet.setRepeatingRows(new CellRangeAddress(2, 2, -1, -1));
        sheet.setPrintGridlines(false);
    }

    private String valueOrBlank(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
