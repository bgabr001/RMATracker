package com.harpenterprises.rmatracker.storage;

import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RmaRepositoryTest {

    private final RmaRepository repository =
            new RmaRepository();

    private final List<String> testRmaNumbers =
            new ArrayList<>();

    @AfterEach
    void cleanUp() throws SQLException {
        for (String rmaNumber : testRmaNumbers) {
            repository.delete(rmaNumber);
        }

        testRmaNumbers.clear();
    }

    @Test
    void saveAndFindRma() throws SQLException {
        String rmaNumber =
                createUniqueRmaNumber("SAVE");

        RmaRecord record = new RmaRecord(
                rmaNumber,
                LocalDate.of(2026, 7, 13),
                null,
                Status.IN_REPAIR,
                "OUT-123",
                "",
                "Repository save test"
        );

        repository.save(record);

        assertTrue(
                repository.existsByRmaNumber(rmaNumber)
        );

        Optional<RmaRecord> savedRecord =
                repository.findByRmaNumber(rmaNumber);

        assertTrue(savedRecord.isPresent());

        assertEquals(
                rmaNumber,
                savedRecord.orElseThrow().getRmaNumber()
        );

        assertEquals(
                Status.IN_REPAIR,
                savedRecord.orElseThrow().getStatus()
        );

        assertEquals(
                "OUT-123",
                savedRecord.orElseThrow()
                        .getOutgoingTrackingNumber()
        );

        assertEquals(
                "Repository save test",
                savedRecord.orElseThrow().getNotes()
        );
    }

    @Test
    void deleteRma() throws SQLException {
        String rmaNumber =
                createUniqueRmaNumber("DELETE");

        RmaRecord record = new RmaRecord(
                rmaNumber,
                LocalDate.of(2026, 7, 13),
                null,
                Status.IN_REPAIR,
                "",
                "",
                "Repository delete test"
        );

        repository.save(record);

        assertTrue(
                repository.existsByRmaNumber(rmaNumber)
        );

        boolean deleted =
                repository.delete(rmaNumber);

        testRmaNumbers.remove(rmaNumber);

        assertTrue(deleted);

        assertFalse(
                repository.existsByRmaNumber(rmaNumber)
        );
    }

    @Test
    void updateStatus() throws SQLException {
        String rmaNumber =
                createUniqueRmaNumber("UPDATE");

        RmaRecord record = new RmaRecord(
                rmaNumber,
                LocalDate.of(2026, 7, 13),
                null,
                Status.IN_REPAIR,
                "",
                "",
                "Before update"
        );

        repository.save(record);

        record.setStatus(Status.COMPLETED);
        record.setNotes("After update");

        repository.update(record);

        RmaRecord loadedRecord =
                repository.findByRmaNumber(rmaNumber)
                        .orElseThrow();

        assertEquals(
                Status.COMPLETED,
                loadedRecord.getStatus()
        );

        assertEquals(
                "After update",
                loadedRecord.getNotes()
        );
    }

    @Test
    void searchFindsRmaNumber() throws SQLException {
        String rmaNumber =
                createUniqueRmaNumber("SEARCH");

        RmaRecord record = new RmaRecord(
                rmaNumber,
                LocalDate.of(2026, 7, 13),
                null,
                Status.IN_REPAIR,
                "",
                "",
                "Search test"
        );

        repository.save(record);

        List<RmaRecord> results =
                repository.search(rmaNumber);

        assertFalse(results.isEmpty());

        assertTrue(
                results.stream().anyMatch(
                        result -> rmaNumber.equals(
                                result.getRmaNumber()
                        )
                )
        );
    }

    @Test
    void existsReturnsFalseForMissingRma()
            throws SQLException {

        String missingRmaNumber =
                "MISSING-" + System.nanoTime();

        assertFalse(
                repository.existsByRmaNumber(
                        missingRmaNumber
                )
        );
    }

    private String createUniqueRmaNumber(
            String testName
    ) {
        String rmaNumber =
                "TEST-"
                        + testName
                        + "-"
                        + System.nanoTime();

        testRmaNumbers.add(rmaNumber);

        return rmaNumber;
    }
}