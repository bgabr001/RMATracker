package com.harpenterprises.rmatracker.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reads text-based PDFs and compares RMA candidates in the filename and document text. */
public final class RmaPdfDetectionService {
    private final RmaFileDetectionService filenames = new RmaFileDetectionService();

    public enum Status { MATCH, CONTENT_ONLY, FILENAME_ONLY, MISMATCH, AMBIGUOUS, NOT_FOUND }

    public record Result(List<String> filenameNumbers, List<String> pdfNumbers, Status status) {
        public Optional<String> verifiedRma() {
            return status == Status.MATCH || status == Status.CONTENT_ONLY
                    ? Optional.of(pdfNumbers.get(0)) : Optional.empty();
        }
    }

    public Result analyze(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!Files.isRegularFile(file) || !Files.isReadable(file))
            throw new IOException("PDF does not exist or cannot be read: " + file);
        if (!file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf"))
            throw new IOException("Only PDF files are supported: " + file);
        List<String> fromName = filenames.detectAll(file);
        String content;
        try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
            content = new PDFTextStripper().getText(pdf);
        }
        return analyzeText(fromName, content);
    }

    /** Also usable for unit tests without disk I/O. */
    public Result analyzeText(String filename, String pdfText) {
        return analyzeText(filenames.detectAll(filename), pdfText);
    }

    private Result analyzeText(List<String> fromName, String pdfText) {
        Objects.requireNonNull(pdfText, "pdfText");
        List<String> fromPdf = filenames.detectAll(pdfText);
        Status status;
        if (fromName.size() > 1 || fromPdf.size() > 1) status = Status.AMBIGUOUS;
        else if (fromName.isEmpty() && fromPdf.isEmpty()) status = Status.NOT_FOUND;
        else if (fromPdf.isEmpty()) status = Status.FILENAME_ONLY;
        else if (fromName.isEmpty()) status = Status.CONTENT_ONLY;
        else status = fromName.get(0).equals(fromPdf.get(0)) ? Status.MATCH : Status.MISMATCH;
        return new Result(List.copyOf(fromName), List.copyOf(fromPdf), status);
    }
}
