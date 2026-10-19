package com.harpenterprises.rmatracker.service;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RmaDatabaseMatchingServiceTest {
    private final RmaPdfDetectionService pdf = new RmaPdfDetectionService();

    @Test void verifiedButMissingInDatabase() throws SQLException {
        var matcher = new RmaDatabaseMatchingService(number -> Optional.empty());
        var result = matcher.match(pdf.analyzeText("V046003_ECC.pdf", "RMA No. V046003"));
        assertEquals(RmaDatabaseMatchingService.Status.NOT_IN_DATABASE, result.status());
        assertEquals("V046003", result.rmaNumber());
    }

    @Test void conflictingNumbersNeverQueryDatabase() throws SQLException {
        AtomicInteger calls = new AtomicInteger();
        var matcher = new RmaDatabaseMatchingService(number -> { calls.incrementAndGet(); return Optional.empty(); });
        var result = matcher.match(pdf.analyzeText("V046155_quote.pdf", "RMA No. V046088"));
        assertEquals(RmaDatabaseMatchingService.Status.NEEDS_REVIEW, result.status());
        assertEquals(0, calls.get());
    }

    @Test void filenameOnlyRequiresReview() throws SQLException {
        var matcher = new RmaDatabaseMatchingService(number -> { fail("Should not query"); return Optional.empty(); });
        assertEquals(RmaDatabaseMatchingService.Status.NEEDS_REVIEW,
                matcher.match(pdf.analyzeText("V046156_quote.pdf", "No RMA number here")).status());
    }

    @Test void contentOnlyCanBeLookedUp() throws SQLException {
        var matcher = new RmaDatabaseMatchingService(number -> {
            assertEquals("V046156", number); return Optional.empty();
        });
        assertEquals(RmaDatabaseMatchingService.Status.NOT_IN_DATABASE,
                matcher.match(pdf.analyzeText("document.pdf", "RMA No. V046156")).status());
    }
}
