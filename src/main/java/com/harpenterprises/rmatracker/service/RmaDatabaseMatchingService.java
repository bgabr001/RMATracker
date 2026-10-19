package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.storage.RmaRepository;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** Read-only matching of verified PDF detection results to existing RMA records. */
public final class RmaDatabaseMatchingService {
    @FunctionalInterface public interface Lookup {
        Optional<RmaRecord> find(String rmaNumber) throws SQLException;
    }
    public enum Status { FOUND, NOT_IN_DATABASE, NEEDS_REVIEW }
    public record Result(Status status, String rmaNumber, Optional<RmaRecord> record,
                         RmaPdfDetectionService.Status detectionStatus) {}

    private final Lookup lookup;
    public RmaDatabaseMatchingService() { this(new RmaRepository()::findByRmaNumber); }
    public RmaDatabaseMatchingService(Lookup lookup) { this.lookup = Objects.requireNonNull(lookup); }

    public Result match(RmaPdfDetectionService.Result detection) throws SQLException {
        Objects.requireNonNull(detection, "detection");
        // Filename-only or conflicting identifiers must not be treated as verified.
        Optional<String> verified = detection.verifiedRma();
        if (verified.isEmpty()) {
            return new Result(Status.NEEDS_REVIEW, null, Optional.empty(), detection.status());
        }
        String number = verified.get();
        Optional<RmaRecord> record = lookup.find(number);
        return new Result(record.isPresent() ? Status.FOUND : Status.NOT_IN_DATABASE,
                number, record, detection.status());
    }
}
