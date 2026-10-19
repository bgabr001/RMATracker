package com.harpenterprises.rmatracker.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RmaAutomaticImportIntegrationTest {
    @TempDir Path temp;

    private Path pdf(String filename, String text) throws Exception {
        Path path = temp.resolve(filename);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(40, 700);
                stream.showText(text);
                stream.endText();
            }
            doc.save(path.toFile());
        }
        return path;
    }

    private static final class Harness {
        final AtomicInteger writes = new AtomicInteger();
        final List<String> importedRmas = new ArrayList<>();
        final List<Path> copies = new ArrayList<>();
        final Path storage;
        final RmaAutomaticAttachmentService service;
        Harness(Path storage, Set<String> existing) {
            this.storage = storage;
            service = new RmaAutomaticAttachmentService(
                    new RmaPdfDetectionService()::analyze,
                    detection -> {
                        Optional<String> verified = detection.verifiedRma();
                        if (verified.isEmpty()) return new RmaDatabaseMatchingService.Result(
                                RmaDatabaseMatchingService.Status.NEEDS_REVIEW, null, Optional.empty(), detection.status());
                        String number = verified.get();
                        return new RmaDatabaseMatchingService.Result(
                                existing.contains(number) ? RmaDatabaseMatchingService.Status.FOUND
                                        : RmaDatabaseMatchingService.Status.NOT_IN_DATABASE,
                                number, Optional.empty(), detection.status());
                    },
                    (number, source) -> {
                        Files.createDirectories(storage);
                        Path destination = storage.resolve(number + "_" + writes.incrementAndGet() + ".pdf");
                        Files.copy(source, destination);
                        importedRmas.add(number);
                        copies.add(destination);
                        return null; // Attachment metadata persistence is not exercised by this isolated writer.
                    });
        }
    }

    @Test void twoMatchingPdfsImportIntoSameRmaAndPreserveOriginals() throws Exception {
        Path ecc = pdf("Harp_Enterprises_Inc._V046156_ECC.pdf", "RMA No. V046156");
        Path quote = pdf("V046156_Harp_Repair quote.pdf", "System Version V046156");
        byte[] originalEcc = Files.readAllBytes(ecc);
        byte[] originalQuote = Files.readAllBytes(quote);
        Harness h = new Harness(temp.resolve("stored"), Set.of("V046156"));
        var first = h.service.preview(ecc);
        var second = h.service.preview(quote);
        assertEquals(RmaAutomaticAttachmentService.Status.READY, first.status());
        assertEquals(RmaAutomaticAttachmentService.Status.READY, second.status());
        assertEquals(0, h.writes.get());
        h.service.importConfirmed(first);
        h.service.importConfirmed(second);
        assertEquals(List.of("V046156", "V046156"), h.importedRmas);
        assertEquals(2, h.copies.size());
        assertArrayEquals(originalEcc, Files.readAllBytes(ecc));
        assertArrayEquals(originalQuote, Files.readAllBytes(quote));
        assertArrayEquals(originalEcc, Files.readAllBytes(h.copies.get(0)));
        assertArrayEquals(originalQuote, Files.readAllBytes(h.copies.get(1)));
    }

    @Test void mismatchNeverWrites() throws Exception {
        Path file = pdf("V046155_Harp_repair quote.pdf", "System Version V046088 Hancock");
        Harness h = new Harness(temp.resolve("stored"), Set.of("V046155", "V046088"));
        var result = h.service.preview(file);
        assertEquals(RmaAutomaticAttachmentService.Status.NEEDS_REVIEW, result.status());
        assertEquals(RmaPdfDetectionService.Status.MISMATCH, result.detectionStatus());
        assertThrows(IllegalStateException.class, () -> h.service.importConfirmed(result));
        assertEquals(0, h.writes.get());
    }

    @Test void nonexistentRmaNeverWrites() throws Exception {
        Path file = pdf("V999999_quote.pdf", "RMA No. V999999");
        Harness h = new Harness(temp.resolve("stored"), Set.of("V046156"));
        var result = h.service.preview(file);
        assertEquals(RmaAutomaticAttachmentService.Status.NOT_IN_DATABASE, result.status());
        assertThrows(IllegalStateException.class, () -> h.service.importConfirmed(result));
        assertEquals(0, h.writes.get());
    }

    @Test void ambiguousContentNeverWrites() throws Exception {
        Path file = pdf("V046156_quote.pdf", "RMA V046156 and RMA V046003");
        Harness h = new Harness(temp.resolve("stored"), Set.of("V046156", "V046003"));
        var result = h.service.preview(file);
        assertEquals(RmaPdfDetectionService.Status.AMBIGUOUS, result.detectionStatus());
        assertFalse(result.canImport());
        assertEquals(0, h.writes.get());
    }

    @Test void changingPdfAfterPreviewBlocksImport() throws Exception {
        Path file = pdf("V046156_quote.pdf", "RMA No. V046156");
        Harness h = new Harness(temp.resolve("stored"), Set.of("V046156"));
        var result = h.service.preview(file);
        Path changed = pdf("replacement.pdf", "RMA No. V046003");
        Files.copy(changed, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertThrows(IllegalStateException.class, () -> h.service.importConfirmed(result));
        assertEquals(0, h.writes.get());
    }

    @Test void importingSameFileTwiceCurrentlyCreatesTwoCopies() throws Exception {
        Path file = pdf("V046156_quote.pdf", "RMA No. V046156");
        Harness h = new Harness(temp.resolve("stored"), Set.of("V046156"));
        var result = h.service.preview(file);
        h.service.importConfirmed(result);
        h.service.importConfirmed(result);
        assertEquals(2, h.writes.get(), "Current implementation does not prevent duplicate imports");
    }
}
