package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RepairItem;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service responsible for reading RMA machine information
 * from Microsoft Excel files.
 *
 * Phase 2A:
 * Excel Import System
 *
 * This class DOES NOT save anything to the database.
 *
 * Its only job is:
 *
 *      Excel File
 *          ↓
 *      Read Spreadsheet
 *          ↓
 *      Validate Rows
 *          ↓
 *      Create RepairItem objects
 *          ↓
 *      Return List<RepairItem>
 *
 * Later phases can decide whether those RepairItems
 * are added to:
 *
 *      - A new RMA
 *      - An existing RMA
 *      - A drag-and-drop import
 */
public class ExcelRmaImportService {

    // ---------------------------------------------------------
    // STANDARD INTERNAL FIELD NAMES
    // ---------------------------------------------------------

    private static final String COUNTY =
            "county";

    private static final String MACHINE_TYPE =
            "machine type";

    private static final String SERIAL_NUMBER =
            "serial number";

    private static final String VERSION =
            "version";

    private static final String PROBLEM_DESCRIPTION =
            "problem description";

    private static final String REPAIR_DESCRIPTION =
            "repair description";

    private static final String RECEIVED =
            "received";


    // ---------------------------------------------------------
    // EXCEL DATA FORMATTER
    // ---------------------------------------------------------

    /**
     * Apache POI DataFormatter allows us to read
     * Excel values the way they appear in the spreadsheet.
     *
     * This is especially useful for serial numbers because
     * we do not want Excel converting them into scientific
     * notation or numeric values.
     */
    private final DataFormatter dataFormatter;


    // ---------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------

    public ExcelRmaImportService() {

        dataFormatter =
                new DataFormatter();
    }


    // =========================================================
    // PUBLIC IMPORT METHOD
    // =========================================================

    /**
     * Reads an Excel file and converts spreadsheet rows
     * into RepairItem objects.
     *
     * Nothing is saved to the RMA database here.
     *
     * @param file Excel .xlsx or .xls file
     * @return imported RepairItem objects
     * @throws IOException if the Excel file cannot be read
     */
    public List<RepairItem> importRepairItems(
            File file
    ) throws IOException {

        validateFile(file);

        try (
                FileInputStream inputStream =
                        new FileInputStream(file);

                Workbook workbook =
                        WorkbookFactory.create(
                                inputStream
                        )
        ) {

            if (workbook.getNumberOfSheets() == 0) {

                throw new IOException(
                        "The Excel workbook does not contain any worksheets."
                );
            }

            Sheet sheet =
                    workbook.getSheetAt(0);

            Row headerRow =
                    findHeaderRow(sheet);

            if (headerRow == null) {

                throw new IOException(
                        "Could not find the RMA Excel headers.\n\n"
                                + "Required columns:\n"
                                + "• County\n"
                                + "• Serial Number"
                );
            }

            Map<String, Integer> columns =
                    readColumnIndexes(
                            headerRow
                    );

            validateRequiredColumns(
                    columns
            );

            return readRepairItems(
                    sheet,
                    headerRow.getRowNum(),
                    columns
            );

        } catch (IOException e) {

            throw e;

        } catch (Exception e) {

            throw new IOException(
                    "RMA Tracker could not read the Excel file.\n\n"
                            + "File: "
                            + file.getName()
                            + "\n\n"
                            + "Reason: "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // FILE VALIDATION
    // =========================================================

    /**
     * Checks whether the selected file is a valid Excel file.
     */
    private void validateFile(
            File file
    ) throws IOException {

        if (file == null) {

            throw new IOException(
                    "No Excel file was selected."
            );
        }


        if (!file.exists()) {

            throw new IOException(
                    "The selected Excel file does not exist:\n"
                            + file.getAbsolutePath()
            );
        }


        if (!file.isFile()) {

            throw new IOException(
                    "The selected path is not a file."
            );
        }


        String fileName =
                file.getName()
                        .toLowerCase(
                                Locale.ROOT
                        );


        if (
                !fileName.endsWith(".xlsx")
                        && !fileName.endsWith(".xls")
        ) {

            throw new IOException(
                    "Only Excel .xlsx or .xls files can be imported."
            );
        }
    }


    // =========================================================
    // HEADER ROW DETECTION
    // =========================================================

    /**
     * Searches the first 25 rows for an Excel header row.
     *
     * For your RMA spreadsheets, the minimum identifying
     * columns are:
     *
     * County
     * Serial Number
     *
     * Example:
     *
     * COUNTY | SERIAL | ISSUE | Firmware
     */
    private Row findHeaderRow(
            Sheet sheet
    ) {

        int lastRow =
                Math.min(
                        sheet.getLastRowNum(),
                        25
                );


        for (
                int rowIndex = 0;
                rowIndex <= lastRow;
                rowIndex++
        ) {

            Row row =
                    sheet.getRow(
                            rowIndex
                    );

            if (row == null) {
                continue;
            }


            boolean hasCounty =
                    false;

            boolean hasSerialNumber =
                    false;


            for (Cell cell : row) {

                String value =
                        normalizeHeader(
                                getCellValue(
                                        cell
                                )
                        );


                if (
                        isCountyHeader(
                                value
                        )
                ) {

                    hasCounty =
                            true;
                }


                if (
                        isSerialNumberHeader(
                                value
                        )
                ) {

                    hasSerialNumber =
                            true;
                }
            }


            /*
             * We consider the row a valid header when
             * both County and Serial Number are found.
             */
            if (
                    hasCounty
                            && hasSerialNumber
            ) {

                return row;
            }
        }


        return null;
    }


    // =========================================================
    // COLUMN MAPPING
    // =========================================================

    /**
     * Determines which spreadsheet column belongs to each
     * RMA Tracker RepairItem field.
     */
    private Map<String, Integer> readColumnIndexes(
            Row headerRow
    ) {

        Map<String, Integer> columns =
                new HashMap<>();


        for (Cell cell : headerRow) {

            String header =
                    normalizeHeader(
                            getCellValue(
                                    cell
                            )
                    );


            int columnIndex =
                    cell.getColumnIndex();


            // ---------------------------------------------
            // COUNTY
            // ---------------------------------------------

            if (
                    isCountyHeader(
                            header
                    )
            ) {

                columns.put(
                        COUNTY,
                        columnIndex
                );
            }


            // ---------------------------------------------
            // MACHINE TYPE
            // ---------------------------------------------

            else if (
                    isMachineTypeHeader(
                            header
                    )
            ) {

                columns.put(
                        MACHINE_TYPE,
                        columnIndex
                );
            }


            // ---------------------------------------------
            // SERIAL NUMBER
            // ---------------------------------------------

            else if (
                    isSerialNumberHeader(
                            header
                    )
            ) {

                columns.put(
                        SERIAL_NUMBER,
                        columnIndex
                );
            }


            // ---------------------------------------------
            // VERSION / FIRMWARE
            // ---------------------------------------------

            else if (
                    isVersionHeader(
                            header
                    )
            ) {

                columns.put(
                        VERSION,
                        columnIndex
                );
            }


            // ---------------------------------------------
            // PROBLEM / ISSUE
            // ---------------------------------------------

            else if (
                    isProblemDescriptionHeader(
                            header
                    )
            ) {

                columns.put(
                        PROBLEM_DESCRIPTION,
                        columnIndex
                );
            }


            // ---------------------------------------------
            // REPAIR DESCRIPTION
            // ---------------------------------------------

            else if (
                    isRepairDescriptionHeader(
                            header
                    )
            ) {

                columns.put(
                        REPAIR_DESCRIPTION,
                        columnIndex
                );
            }


            // ---------------------------------------------
            // RECEIVED
            // ---------------------------------------------

            else if (
                    isReceivedHeader(
                            header
                    )
            ) {

                columns.put(
                        RECEIVED,
                        columnIndex
                );
            }
        }


        return columns;
    }


    // =========================================================
    // REQUIRED COLUMN VALIDATION
    // =========================================================

    /**
     * Defines the minimum Excel data required for an import.
     *
     * Your real spreadsheets do not always contain Machine Type,
     * so Machine Type is intentionally NOT required here.
     *
     * Required:
     *
     * County
     * Serial Number
     */
    private void validateRequiredColumns(
            Map<String, Integer> columns
    ) throws IOException {

        List<String> missing =
                new ArrayList<>();


        if (
                !columns.containsKey(
                        COUNTY
                )
        ) {

            missing.add(
                    "County"
            );
        }


        if (
                !columns.containsKey(
                        SERIAL_NUMBER
                )
        ) {

            missing.add(
                    "Serial Number"
            );
        }


        if (
                !missing.isEmpty()
        ) {

            throw new IOException(
                    "The Excel file is missing required column(s): "
                            + String.join(
                            ", ",
                            missing
                    )
            );
        }
    }


    // =========================================================
    // READ REPAIR ITEMS
    // =========================================================

    /**
     * Reads each spreadsheet row and creates a RepairItem.
     */
    private List<RepairItem> readRepairItems(
            Sheet sheet,
            int headerRowNumber,
            Map<String, Integer> columns
    ) throws IOException {

        List<RepairItem> repairItems =
                new ArrayList<>();


        /*
         * Data begins on the row immediately after the header.
         */
        for (
                int rowIndex =
                headerRowNumber + 1;

                rowIndex <=
                        sheet.getLastRowNum();

                rowIndex++
        ) {

            Row row =
                    sheet.getRow(
                            rowIndex
                    );


            // Skip completely empty rows.
            if (
                    row == null
                            || isEmptyRow(
                            row
                    )
            ) {

                continue;
            }


            // ---------------------------------------------
            // READ EACH FIELD
            // ---------------------------------------------

            String county =
                    getValue(
                            row,
                            columns,
                            COUNTY
                    );


            String machineType =
                    getValue(
                            row,
                            columns,
                            MACHINE_TYPE
                    );


            String serialNumber =
                    getValue(
                            row,
                            columns,
                            SERIAL_NUMBER
                    );


            String version =
                    getValue(
                            row,
                            columns,
                            VERSION
                    );


            String problemDescription =
                    getValue(
                            row,
                            columns,
                            PROBLEM_DESCRIPTION
                    );


            String repairDescription =
                    getValue(
                            row,
                            columns,
                            REPAIR_DESCRIPTION
                    );


            String receivedText =
                    getValue(
                            row,
                            columns,
                            RECEIVED
                    );


            // ---------------------------------------------
            // SKIP COMPLETELY MEANINGLESS ROWS
            // ---------------------------------------------

            if (
                    county.isBlank()
                            && machineType.isBlank()
                            && serialNumber.isBlank()
                            && version.isBlank()
                            && problemDescription.isBlank()
                            && repairDescription.isBlank()
            ) {

                continue;
            }


            // ---------------------------------------------
            // VALIDATE COUNTY
            // ---------------------------------------------

            if (
                    county.isBlank()
            ) {

                throw new IOException(
                        "Excel row "
                                + (rowIndex + 1)
                                + " is missing County."
                );
            }


            // ---------------------------------------------
            // VALIDATE SERIAL NUMBER
            // ---------------------------------------------

            if (
                    serialNumber.isBlank()
            ) {

                throw new IOException(
                        "Excel row "
                                + (rowIndex + 1)
                                + " is missing Serial Number."
                );
            }


            // ---------------------------------------------
            // RECEIVED CHECKBOX VALUE
            // ---------------------------------------------

            boolean received =
                    parseReceived(
                            receivedText
                    );


            // ---------------------------------------------
            // CREATE REPAIR ITEM
            // ---------------------------------------------

            RepairItem repairItem =
                    new RepairItem(
                            county,
                            machineType,
                            serialNumber,
                            version,
                            problemDescription,
                            repairDescription,
                            received
                    );


            repairItems.add(
                    repairItem
            );
        }


        // ---------------------------------------------
        // MAKE SURE SOMETHING WAS IMPORTED
        // ---------------------------------------------

        if (
                repairItems.isEmpty()
        ) {

            throw new IOException(
                    "No repair items were found in the Excel file."
            );
        }


        return repairItems;
    }


    // =========================================================
    // GET CELL VALUE
    // =========================================================

    /**
     * Reads a value from a row based on our internal
     * field-to-column mapping.
     *
     * If the Excel spreadsheet does not contain that optional
     * column, an empty String is returned.
     */
    private String getValue(
            Row row,
            Map<String, Integer> columns,
            String fieldName
    ) {

        Integer columnIndex =
                columns.get(
                        fieldName
                );


        /*
         * Optional column was not present in the spreadsheet.
         */
        if (
                columnIndex == null
        ) {

            return "";
        }


        Cell cell =
                row.getCell(
                        columnIndex
                );


        return getCellValue(
                cell
        );
    }


    /**
     * Converts an Excel cell into a String.
     */
    private String getCellValue(
            Cell cell
    ) {

        if (
                cell == null
        ) {

            return "";
        }


        return dataFormatter
                .formatCellValue(
                        cell
                )
                .trim();
    }


    // =========================================================
    // EMPTY ROW CHECK
    // =========================================================

    /**
     * Determines whether an Excel row is completely empty.
     */
    private boolean isEmptyRow(
            Row row
    ) {

        int firstCell =
                row.getFirstCellNum();


        int lastCell =
                row.getLastCellNum();


        if (
                firstCell < 0
                        || lastCell < 0
        ) {

            return true;
        }


        for (
                int index = firstCell;
                index < lastCell;
                index++
        ) {

            Cell cell =
                    row.getCell(
                            index
                    );


            if (
                    cell != null
                            && cell.getCellType()
                            != CellType.BLANK
                            && !getCellValue(
                            cell
                    ).isBlank()
            ) {

                return false;
            }
        }


        return true;
    }


    // =========================================================
    // RECEIVED VALUE CONVERSION
    // =========================================================

    /**
     * Converts common spreadsheet representations of
     * "received" into a boolean.
     *
     * Examples considered TRUE:
     *
     * Yes
     * Y
     * True
     * X
     * 1
     * Received
     *
     * Blank cells default to false.
     */
    private boolean parseReceived(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return false;
        }


        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        return normalized.equals("yes")
                || normalized.equals("y")
                || normalized.equals("true")
                || normalized.equals("x")
                || normalized.equals("1")
                || normalized.equals("received");
    }


    // =========================================================
    // HEADER NORMALIZATION
    // =========================================================

    /**
     * Normalizes Excel headings so that variations such as:
     *
     * SERIAL
     * Serial
     * serial
     * Serial_Number
     * Serial-Number
     *
     * can all be compared reliably.
     */
    private String normalizeHeader(
            String header
    ) {

        if (
                header == null
        ) {

            return "";
        }


        return header
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "_",
                        " "
                )
                .replace(
                        "-",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                );
    }


    // =========================================================
    // HEADER MATCHING
    // =========================================================

    private boolean isCountyHeader(
            String value
    ) {

        return value.equals(
                "county"
        )
                || value.equals(
                "county name"
        );
    }


    private boolean isMachineTypeHeader(
            String value
    ) {

        return value.equals(
                "machine type"
        )
                || value.equals(
                "machine"
        )
                || value.equals(
                "type"
        )
                || value.equals(
                "equipment type"
        );
    }


    private boolean isSerialNumberHeader(
            String value
    ) {

        return value.equals(
                "serial"
        )
                || value.equals(
                "serial number"
        )
                || value.equals(
                "serial #"
        )
                || value.equals(
                "serial no"
        )
                || value.equals(
                "serial no."
        );
    }


    private boolean isVersionHeader(
            String value
    ) {

        return value.equals(
                "version"
        )
                || value.equals(
                "firmware"
        )
                || value.equals(
                "firmware version"
        )
                || value.equals(
                "software version"
        );
    }


    private boolean isProblemDescriptionHeader(
            String value
    ) {

        return value.equals(
                "problem"
        )
                || value.equals(
                "problem description"
        )
                || value.equals(
                "issue"
        )
                || value.equals(
                "issue description"
        );
    }


    private boolean isRepairDescriptionHeader(
            String value
    ) {

        return value.equals(
                "repair"
        )
                || value.equals(
                "repair description"
        )
                || value.equals(
                "repair notes"
        )
                || value.equals(
                "resolution"
        );
    }


    private boolean isReceivedHeader(
            String value
    ) {

        return value.equals(
                "received"
        )
                || value.equals(
                "received?"
        )
                || value.equals(
                "returned"
        );
    }
}