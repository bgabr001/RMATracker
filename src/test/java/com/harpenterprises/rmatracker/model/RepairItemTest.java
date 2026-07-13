package com.harpenterprises.rmatracker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepairItemTest {

    @Test
    void constructorStoresValues() {
        RepairItem item = new RepairItem(
                "Jefferson",
                "Scanner",
                "12345",
                "v2",
                "Does not boot",
                "Main board replaced"
        );

        assertEquals(
                "Jefferson",
                item.getCounty()
        );

        assertEquals(
                "Scanner",
                item.getMachineType()
        );

        assertEquals(
                "12345",
                item.getSerialNumber()
        );

        assertEquals(
                "v2",
                item.getVersion()
        );

        assertEquals(
                "Does not boot",
                item.getProblemDescription()
        );

        assertEquals(
                "Main board replaced",
                item.getRepairDescription()
        );
    }
}