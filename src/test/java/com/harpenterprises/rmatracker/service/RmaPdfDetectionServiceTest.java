package com.harpenterprises.rmatracker.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RmaPdfDetectionServiceTest {
    private final RmaPdfDetectionService service = new RmaPdfDetectionService();
    @TempDir Path temp;

    @Test void matchingEcc() {
        var result = service.analyzeText("Harp_Enterprises_Inc._V046003_ECC.pdf", "RMA No. V046003");
        assertEquals(RmaPdfDetectionService.Status.MATCH, result.status());
        assertEquals("V046003", result.verifiedRma().orElseThrow());
    }
    @Test void conflictingQuote() {
        var result = service.analyzeText("V046155_Harp_repair quote.pdf", "System Version V046088 (Hancock)");
        assertEquals(RmaPdfDetectionService.Status.MISMATCH, result.status());
        assertTrue(result.verifiedRma().isEmpty());
    }
    @Test void repeatedRmaIsNotAmbiguous() {
        assertEquals(RmaPdfDetectionService.Status.MATCH,
                service.analyzeText("V046156_ECC.pdf", "V046156 V046156").status());
    }
    @Test void multipleContentNumbersAreAmbiguous() {
        assertEquals(RmaPdfDetectionService.Status.AMBIGUOUS,
                service.analyzeText("V046003.pdf", "V046003 V046155").status());
    }
    @Test void contentOnly() {
        assertEquals("V046003", service.analyzeText("document.pdf", "RMA No. V046003")
                .verifiedRma().orElseThrow());
    }
    @Test void filenameOnlyIsNotVerified() {
        var result = service.analyzeText("V046003.pdf", "No readable RMA number");
        assertEquals(RmaPdfDetectionService.Status.FILENAME_ONLY, result.status());
        assertTrue(result.verifiedRma().isEmpty());
    }
    @Test void notFound() {
        assertEquals(RmaPdfDetectionService.Status.NOT_FOUND,
                service.analyzeText("document.pdf", "no number").status());
    }
    @Test void extractsRealPdfText() throws Exception {
        Path pdfFile = temp.resolve("V046003_test.pdf");
        try (PDDocument pdf = new PDDocument()) {
            pdf.addPage(new PDPage());
            try (PDPageContentStream stream = new PDPageContentStream(pdf, pdf.getPage(0))) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("RMA No. V046003");
                stream.endText();
            }
            pdf.save(pdfFile.toFile());
        }
        assertEquals(RmaPdfDetectionService.Status.MATCH, service.analyze(pdfFile).status());
    }
}
