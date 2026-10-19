package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RmaAttachment;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

/** Step 8D: import only a PDF whose content has been verified and whose RMA exists. */
public final class RmaAutomaticAttachmentService {
    @FunctionalInterface public interface Detector {
        RmaPdfDetectionService.Result analyze(Path file) throws IOException;
    }
    @FunctionalInterface public interface Matcher {
        RmaDatabaseMatchingService.Result match(RmaPdfDetectionService.Result detection) throws SQLException;
    }
    @FunctionalInterface public interface AttachmentWriter {
        RmaAttachment add(String rmaNumber, Path file) throws IOException, SQLException;
    }
    public enum Status { READY, NEEDS_REVIEW, NOT_IN_DATABASE }
    public record Preview(Path file, Status status, String rmaNumber,
                          RmaPdfDetectionService.Status detectionStatus) {
        public boolean canImport() { return status == Status.READY; }
    }

    private final Detector detector;
    private final Matcher matcher;
    private final AttachmentWriter writer;

    public RmaAutomaticAttachmentService() {
        this(new RmaPdfDetectionService()::analyze,
                new RmaDatabaseMatchingService()::match,
                new RmaAttachmentStorageService()::add);
    }

    public RmaAutomaticAttachmentService(Detector detector, Matcher matcher, AttachmentWriter writer) {
        this.detector = Objects.requireNonNull(detector);
        this.matcher = Objects.requireNonNull(matcher);
        this.writer = Objects.requireNonNull(writer);
    }

    /** Preview does not write to the database or managed attachment storage. */
    public Preview preview(Path file) throws IOException, SQLException {
        Objects.requireNonNull(file, "file");
        RmaPdfDetectionService.Result detection = detector.analyze(file);
        RmaDatabaseMatchingService.Result match = matcher.match(detection);
        Status status = switch (match.status()) {
            case FOUND -> Status.READY;
            case NOT_IN_DATABASE -> Status.NOT_IN_DATABASE;
            case NEEDS_REVIEW -> Status.NEEDS_REVIEW;
        };
        return new Preview(file.toAbsolutePath().normalize(), status,
                match.rmaNumber(), detection.status());
    }

    /** Caller must obtain explicit user confirmation before invoking this method. Revalidates before writing. */
    public RmaAttachment importConfirmed(Preview approved) throws IOException, SQLException {
        Objects.requireNonNull(approved, "approved");
        if (!approved.canImport()) throw new IllegalStateException("File is not approved for automatic attachment");
        Preview current = preview(approved.file());
        if (!current.canImport() || !Objects.equals(current.rmaNumber(), approved.rmaNumber())) {
            throw new IllegalStateException("RMA detection changed since preview; review the file again");
        }
        return writer.add(current.rmaNumber(), current.file());
    }
}
