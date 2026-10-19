package com.harpenterprises.rmatracker.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RmaAutomaticAttachmentServiceTest {
    private static RmaPdfDetectionService.Result detection(String file, String text) {
        return new RmaPdfDetectionService().analyzeText(file, text);
    }
    @Test void matchingDocumentCanBePreviewedWithoutWriting() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        var service = new RmaAutomaticAttachmentService(
            p -> detection(p.getFileName().toString(), "RMA No. V046003"),
            d -> new RmaDatabaseMatchingService.Result(RmaDatabaseMatchingService.Status.FOUND,
                    "V046003", Optional.empty(), d.status()),
            (r,p) -> { writes.incrementAndGet(); return null; });
        var preview = service.preview(Path.of("V046003_quote.pdf"));
        assertTrue(preview.canImport());
        assertEquals(0, writes.get());
        service.importConfirmed(preview);
        assertEquals(1, writes.get());
    }
    @Test void conflictingDocumentIsBlocked() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        var service = new RmaAutomaticAttachmentService(
            p -> detection("V046155_quote.pdf", "RMA No. V046088"),
            d -> new RmaDatabaseMatchingService.Result(RmaDatabaseMatchingService.Status.NEEDS_REVIEW,
                    null, Optional.empty(), d.status()),
            (r,p) -> { writes.incrementAndGet(); return null; });
        var preview = service.preview(Path.of("V046155_quote.pdf"));
        assertEquals(RmaAutomaticAttachmentService.Status.NEEDS_REVIEW, preview.status());
        assertThrows(IllegalStateException.class, () -> service.importConfirmed(preview));
        assertEquals(0, writes.get());
    }
    @Test void nonexistentRmaIsBlocked() throws Exception {
        var service = new RmaAutomaticAttachmentService(
            p -> detection("V999999_quote.pdf", "RMA V999999"),
            d -> new RmaDatabaseMatchingService.Result(RmaDatabaseMatchingService.Status.NOT_IN_DATABASE,
                    "V999999", Optional.empty(), d.status()),
            (r,p) -> fail("must not write"));
        var preview = service.preview(Path.of("V999999_quote.pdf"));
        assertEquals(RmaAutomaticAttachmentService.Status.NOT_IN_DATABASE, preview.status());
        assertFalse(preview.canImport());
    }
    @Test void changedDetectionIsBlockedBeforeWrite() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var service = new RmaAutomaticAttachmentService(
            p -> calls.incrementAndGet() == 1
                ? detection("V046003_quote.pdf", "RMA V046003")
                : detection("V046003_quote.pdf", "RMA V046155"),
            d -> d.verifiedRma().map(r -> new RmaDatabaseMatchingService.Result(
                    RmaDatabaseMatchingService.Status.FOUND, r, Optional.empty(), d.status()))
                .orElseGet(() -> new RmaDatabaseMatchingService.Result(
                    RmaDatabaseMatchingService.Status.NEEDS_REVIEW, null, Optional.empty(), d.status())),
            (r,p) -> fail("must not write"));
        var preview = service.preview(Path.of("V046003_quote.pdf"));
        assertThrows(IllegalStateException.class, () -> service.importConfirmed(preview));
    }
}
