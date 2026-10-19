package com.harpenterprises.rmatracker.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Metadata for a file attached to an RMA. File bytes are stored outside SQLite. */
public class RmaAttachment {
    private long id;
    private String rmaNumber;
    private String originalFileName;
    private String storedFileName;
    private String storagePath;
    private long fileSize;
    private String contentType;
    private LocalDateTime dateAdded;

    public RmaAttachment() {
        this.dateAdded = LocalDateTime.now();
    }

    public RmaAttachment(long id, String rmaNumber, String originalFileName,
                         String storedFileName, String storagePath, long fileSize,
                         String contentType, LocalDateTime dateAdded) {
        this.id = id;
        this.rmaNumber = Objects.requireNonNull(rmaNumber, "rmaNumber");
        this.originalFileName = Objects.requireNonNull(originalFileName, "originalFileName");
        this.storedFileName = Objects.requireNonNull(storedFileName, "storedFileName");
        this.storagePath = Objects.requireNonNull(storagePath, "storagePath");
        if (fileSize < 0) throw new IllegalArgumentException("fileSize must be nonnegative");
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.dateAdded = Objects.requireNonNull(dateAdded, "dateAdded");
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getRmaNumber() { return rmaNumber; }
    public void setRmaNumber(String rmaNumber) { this.rmaNumber = rmaNumber; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) {
        if (fileSize < 0) throw new IllegalArgumentException("fileSize must be nonnegative");
        this.fileSize = fileSize;
    }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public LocalDateTime getDateAdded() { return dateAdded; }
    public void setDateAdded(LocalDateTime dateAdded) { this.dateAdded = dateAdded; }

    @Override
    public String toString() {
        return originalFileName + " (RMA " + rmaNumber + ")";
    }
}
