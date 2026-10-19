package com.harpenterprises.rmatracker.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class RmaFileDetectionServiceTest {
    private final RmaFileDetectionService service = new RmaFileDetectionService();

    @Test void detectsEccFilename() {
        assertEquals(Optional.of("V046003"), service.detect("Harp_Enterprises_Inc._V046003_ECC.pdf"));
    }
    @Test void detectsQuoteFilename() {
        assertEquals(Optional.of("V046155"), service.detect("V046155_Harp_repair quote.pdf"));
    }
    @Test void detectsThirdRma() {
        assertEquals(Optional.of("V046156"), service.detect(Path.of("V046156_Harp_Repair quote.pdf")));
    }
    @Test void caseInsensitive() {
        assertEquals(Optional.of("V046156"), service.detect("v046156_quote.pdf"));
    }
    @Test void noCandidate() {
        assertTrue(service.detect("invoice.pdf").isEmpty());
    }
    @Test void avoidsPartialMatches() {
        assertTrue(service.detect("AV046003.pdf").isEmpty());
        assertTrue(service.detect("V0460037.pdf").isEmpty());
    }
    @Test void multipleDifferentNumbersAreAmbiguous() {
        assertTrue(service.detect("V046003_V046155.pdf").isEmpty());
        assertEquals(List.of("V046003", "V046155"), service.detectAll("V046003_V046155.pdf"));
    }
    @Test void repeatedSameNumberIsUnambiguous() {
        assertEquals(Optional.of("V046003"), service.detect("V046003_copy_V046003.pdf"));
    }
}
