package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RepairItem;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelRmaImportServiceTest {

    @Test
    void importsSampleExcelHeadersSuccessfully() throws Exception {

        ExcelRmaImportService service =
                new ExcelRmaImportService();

        /*
         * Change this path to wherever you place
         * the sample spreadsheet inside your project.
         *
         * Recommended:
         *
         * src/test/resources/8-28-26.xlsx
         */
        File file =
                new File(
                        "src/test/resources/8-28-26.xlsx"
                );

        assertTrue(
                file.exists(),
                "Sample Excel file could not be found."
        );

        List<RepairItem> items =
                service.importRepairItems(
                        file
                );

        assertNotNull(items);

        assertFalse(
                items.isEmpty(),
                "The Excel file should contain imported machines."
        );
    }


    @Test
    void convertsExcelRowsIntoRepairItems() throws Exception {

        ExcelRmaImportService service =
                new ExcelRmaImportService();

        File file =
                new File(
                        "src/test/resources/8-28-26.xlsx"
                );

        List<RepairItem> items =
                service.importRepairItems(file);

        assertFalse(
                items.isEmpty(),
                "Expected at least one RepairItem."
        );

        System.out.println();
        System.out.println("========================================");
        System.out.println("2A-3 ROW CONVERSION TEST");
        System.out.println("========================================");

        for (int i = 0; i < items.size(); i++) {

            RepairItem item =
                    items.get(i);

            System.out.println();
            System.out.println(
                    "RepairItem " + (i + 1)
            );

            System.out.println(
                    "County: "
                            + item.getCounty()
            );

            System.out.println(
                    "Serial: "
                            + item.getSerialNumber()
            );

            System.out.println(
                    "Machine Type: "
                            + item.getMachineType()
            );

            System.out.println(
                    "Version: "
                            + item.getVersion()
            );

            System.out.println(
                    "Problem: "
                            + item.getProblemDescription()
            );

            System.out.println(
                    "Repair: "
                            + item.getRepairDescription()
            );

            System.out.println(
                    "Received: "
                            + item.isReceived()
            );
        }

        System.out.println();
        System.out.println(
                "2A-3 PASSED: "
                        + items.size()
                        + " RepairItems created."
        );

        System.out.println(
                "========================================"
        );
    }

    @Test
    void rejectsNonExcelFile() {

        ExcelRmaImportService service =
                new ExcelRmaImportService();

        File file =
                new File(
                        "src/test/resources/not-excel.txt"
                );

        IOException exception =
                assertThrows(
                        IOException.class,
                        () ->
                                service.importRepairItems(
                                        file
                                )
                );

        System.out.println();
        System.out.println(
                "2A-4 EXPECTED ERROR:"
        );

        System.out.println(
                exception.getMessage()
        );
    }

    @Test
    void performsFullSampleImport() throws Exception {

        System.out.println();
        System.out.println("========================================");
        System.out.println("2A-5 FULL RMA EXCEL IMPORT");
        System.out.println("========================================");

        ExcelRmaImportService service =
                new ExcelRmaImportService();

        File file =
                new File(
                        "src/test/resources/8-28-26.xlsx"
                );

        assertTrue(
                file.exists(),
                "8-28-26.xlsx was not found."
        );

        List<RepairItem> items =
                service.importRepairItems(
                        file
                );

        assertNotNull(items);

        assertFalse(
                items.isEmpty()
        );

        for (int i = 0; i < items.size(); i++) {

            RepairItem item =
                    items.get(i);

            System.out.println();
            System.out.println(
                    "Machine #" + (i + 1)
            );

            System.out.println(
                    "County:   "
                            + item.getCounty()
            );

            System.out.println(
                    "Serial:   "
                            + item.getSerialNumber()
            );

            System.out.println(
                    "Type:     "
                            + item.getMachineType()
            );

            System.out.println(
                    "Firmware: "
                            + item.getVersion()
            );

            System.out.println(
                    "Issue:    "
                            + item.getProblemDescription()
            );
        }

        System.out.println();
        System.out.println("----------------------------------------");

        System.out.println(
                "TOTAL MACHINES IMPORTED: "
                        + items.size()
        );

        System.out.println("----------------------------------------");

        System.out.println(
                "2A-5 FULL IMPORT PASSED"
        );

        System.out.println(
                "========================================"
        );
    }

    private File createTestWorkbook(
            String[] headers,
            String[] values
    ) throws Exception {

        File file =
                File.createTempFile(
                        "rma-import-test-",
                        ".xlsx"
                );

        file.deleteOnExit();

        try (
                Workbook workbook =
                        new XSSFWorkbook()
        ) {

            Sheet sheet =
                    workbook.createSheet(
                            "RMA"
                    );

            Row headerRow =
                    sheet.createRow(0);

            for (
                    int i = 0;
                    i < headers.length;
                    i++
            ) {

                headerRow
                        .createCell(i)
                        .setCellValue(
                                headers[i]
                        );
            }


            Row dataRow =
                    sheet.createRow(1);

            for (
                    int i = 0;
                    i < values.length;
                    i++
            ) {

                dataRow
                        .createCell(i)
                        .setCellValue(
                                values[i]
                        );
            }


            try (
                    FileOutputStream outputStream =
                            new FileOutputStream(file)
            ) {

                workbook.write(
                        outputStream
                );
            }
        }

        return file;
    }
}