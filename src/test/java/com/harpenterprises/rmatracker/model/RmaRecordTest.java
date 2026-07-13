package com.harpenterprises.rmatracker.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RmaRecordTest {

    @Test
    void constructorStoresValues() {
        RmaRecord record = new RmaRecord(
                "RMA-100",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 5),
                Status.IN_REPAIR,
                "OUT123",
                "RET456",
                "Testing"
        );

        assertEquals(
                "RMA-100",
                record.getRmaNumber()
        );

        assertEquals(
                LocalDate.of(2026, 1, 1),
                record.getDateSent()
        );

        assertEquals(
                LocalDate.of(2026, 1, 5),
                record.getDateReceived()
        );

        assertEquals(
                Status.IN_REPAIR,
                record.getStatus()
        );

        assertEquals(
                "OUT123",
                record.getOutgoingTrackingNumber()
        );

        assertEquals(
                "RET456",
                record.getReturnTrackingNumber()
        );

        assertEquals(
                "Testing",
                record.getNotes()
        );

        assertNotNull(record.getRepairItems());
        assertTrue(record.getRepairItems().isEmpty());
    }

    @Test
    void statusCanBeChanged() {
        RmaRecord record = new RmaRecord(
                "RMA-101",
                LocalDate.of(2026, 2, 1),
                null,
                Status.IN_REPAIR,
                "",
                "",
                ""
        );

        record.setStatus(Status.COMPLETED);

        assertEquals(
                Status.COMPLETED,
                record.getStatus()
        );
    }
}