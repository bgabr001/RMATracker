package com.harpenterprises.rmatracker.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LargeRmaTest {

    @Test
    void rmaCanContainOneHundredFiftyMachines() {
        RmaRecord record = createLargeRma(150);

        assertNotNull(record.getRepairItems());

        assertEquals(
                150,
                record.getRepairItems().size()
        );
    }

    @Test
    void generatedMachinesHaveUniqueSerialNumbers() {
        RmaRecord record = createLargeRma(150);

        Set<String> serialNumbers = new HashSet<>();

        for (RepairItem item : record.getRepairItems()) {
            serialNumbers.add(item.getSerialNumber());
        }

        assertEquals(
                150,
                serialNumbers.size()
        );
    }

    @Test
    void generatedMachinesContainExpectedValues() {
        RmaRecord record = createLargeRma(150);

        RepairItem firstItem =
                record.getRepairItems().get(0);

        RepairItem lastItem =
                record.getRepairItems().get(149);

        assertEquals(
                "TEST-SERIAL-0001",
                firstItem.getSerialNumber()
        );

        assertEquals(
                "TEST-SERIAL-0150",
                lastItem.getSerialNumber()
        );

        assertFalse(firstItem.getCounty().isBlank());
        assertFalse(firstItem.getMachineType().isBlank());
        assertFalse(firstItem.getProblemDescription().isBlank());
        assertFalse(firstItem.getRepairDescription().isBlank());
    }

    @Test
    void largeRmaContainsDifferentMachineTypes() {
        RmaRecord record = createLargeRma(150);

        boolean containsScanner =
                record.getRepairItems()
                        .stream()
                        .anyMatch(item ->
                                "Scanner".equals(
                                        item.getMachineType()
                                )
                        );

        boolean containsDuo =
                record.getRepairItems()
                        .stream()
                        .anyMatch(item ->
                                "Duo".equals(
                                        item.getMachineType()
                                )
                        );

        boolean containsPrint =
                record.getRepairItems()
                        .stream()
                        .anyMatch(item ->
                                "Print".equals(
                                        item.getMachineType()
                                )
                        );

        assertTrue(containsScanner);
        assertTrue(containsDuo);
        assertTrue(containsPrint);
    }

    private RmaRecord createLargeRma(
            int machineCount
    ) {
        RmaRecord record = new RmaRecord(
                "TEST-LARGE-001",
                LocalDate.of(2026, 7, 14),
                null,
                Status.IN_REPAIR,
                "TEST-OUTGOING-001",
                "",
                "Large automatically generated test RMA"
        );

        for (int machineNumber = 1;
             machineNumber <= machineCount;
             machineNumber++) {

            RepairItem item = new RepairItem(
                    createCounty(machineNumber),
                    createMachineType(machineNumber),
                    String.format(
                            "TEST-SERIAL-%04d",
                            machineNumber
                    ),
                    "Version "
                            + (((machineNumber - 1) % 5) + 1),
                    "Problem description for machine "
                            + machineNumber,
                    "Repair description for machine "
                            + machineNumber
            );

            record.addRepairItem(item);
        }

        return record;
    }

    private String createCounty(
            int machineNumber
    ) {
        int countyNumber =
                ((machineNumber - 1) % 20) + 1;

        return "Test County " + countyNumber;
    }

    private String createMachineType(
            int machineNumber
    ) {
        return switch (machineNumber % 5) {
            case 0 -> "Scanner";
            case 1 -> "Duo Standalone";
            case 2 -> "Touch Writer";
            case 3 -> "Duo";
            default -> "Print";
        };
    }
}
