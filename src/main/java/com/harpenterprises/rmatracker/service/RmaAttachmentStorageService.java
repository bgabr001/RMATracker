package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RmaAttachment;
import com.harpenterprises.rmatracker.storage.DatabaseManager;
import com.harpenterprises.rmatracker.storage.RmaAttachmentRepository;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Copies attachment bytes to application-managed storage and records metadata in SQLite. */
public final class RmaAttachmentStorageService {
    private final RmaAttachmentRepository repository;
    private final Path root;

    public RmaAttachmentStorageService() {
        this(new RmaAttachmentRepository(),
                DatabaseManager.getApplicationFolder().toPath().resolve("attachments"));
    }

    public RmaAttachmentStorageService(RmaAttachmentRepository repository, Path root) {
        this.repository = Objects.requireNonNull(repository);
        this.root = Objects.requireNonNull(root).toAbsolutePath().normalize();
    }

    /** The RMA must already exist in the database. */
    public RmaAttachment add(String rmaNumber, Path source) throws IOException, SQLException {
        return add(rmaNumber, source, false);
    }

    /** Explicit user-approved imports may retain identical copies. */
    public RmaAttachment add(String rmaNumber, Path source, boolean allowIdentical) throws IOException, SQLException {
        if (rmaNumber == null || rmaNumber.isBlank()) {
            throw new IllegalArgumentException("RMA number is required");
        }
        Objects.requireNonNull(source, "source");
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new IOException("Not a readable regular file: " + source);
        }
        String original = source.getFileName().toString();
        if (original.isBlank()) throw new IOException("Invalid source filename");

        // Reuse an existing attachment only when its bytes match within the same RMA.
        // A matching filename alone is not sufficient: revised PDFs may share a name.
        if (!allowIdentical) {
            RmaAttachment duplicate = findDuplicate(rmaNumber, source);
            if (duplicate != null) return duplicate;
        }

        // Use a stable numeric database ID, not an editable RMA number, for folder names.
        long rmaId = lookupRmaId(rmaNumber);
        Path folder = root.resolve(Long.toString(rmaId));
        Files.createDirectories(folder);
        String stored = UUID.randomUUID() + extension(original);
        Path target = folder.resolve(stored).normalize();
        if (!target.startsWith(root)) throw new IOException("Unsafe attachment path");

        Files.copy(source, target); // Never overwrite another attachment.
        try {
            RmaAttachment attachment = new RmaAttachment(0, rmaNumber, original, stored,
                    target.toString(), Files.size(target), Files.probeContentType(source),
                    LocalDateTime.now());
            repository.save(attachment);
            return attachment;
        } catch (SQLException | RuntimeException e) {
            try { Files.deleteIfExists(target); } catch (IOException cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }

    /** Returns an existing managed attachment with identical content, if any. */
    public RmaAttachment findDuplicate(String rmaNumber, Path source) throws SQLException, IOException {
        Objects.requireNonNull(source, "source");
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new IOException("Not a readable regular file: " + source);
        }
        long size = Files.size(source);
        for (RmaAttachment existing : repository.findByRmaNumber(rmaNumber)) {
            if (existing.getFileSize() != size) continue;
            Path stored = checkedPath(existing);
            if (Files.isRegularFile(stored) && Files.size(stored) == size
                    && Files.mismatch(source, stored) == -1L) {
                return existing;
            }
        }
        return null;
    }

    public List<RmaAttachment> list(String rmaNumber) throws SQLException {
        return repository.findByRmaNumber(rmaNumber);
    }

    public Path getFile(long attachmentId) throws SQLException, IOException {
        RmaAttachment attachment = repository.findById(attachmentId);
        if (attachment == null) throw new IOException("Attachment not found: " + attachmentId);
        Path path = checkedPath(attachment);
        if (!Files.isRegularFile(path)) throw new IOException("Attachment file missing: " + path);
        return path;
    }

    public void open(long attachmentId) throws SQLException, IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException("Opening files is not supported on this system");
        }
        Desktop.getDesktop().open(getFile(attachmentId).toFile());
    }

    public void showInFinder(long attachmentId) throws SQLException, IOException {
        Path path = getFile(attachmentId);
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException("Opening folders is not supported on this system");
        }
        Desktop.getDesktop().open(path.getParent().toFile());
    }

    /** Removes both the file and its database record. Call after user confirmation in the UI. */
    public boolean remove(long attachmentId) throws SQLException, IOException {
        RmaAttachment attachment = repository.findById(attachmentId);
        if (attachment == null) return false;
        Path path = checkedPath(attachment);
        // Move to a temporary file first so a database failure can restore the original.
        Path quarantine = path.resolveSibling(path.getFileName() + ".deleting-" + UUID.randomUUID());
        boolean moved = Files.exists(path);
        if (moved) Files.move(path, quarantine);
        try {
            boolean deleted = repository.deleteById(attachmentId);
            if (!deleted) {
                if (moved) Files.move(quarantine, path);
                return false;
            }
        } catch (SQLException | RuntimeException e) {
            if (moved) {
                try { Files.move(quarantine, path); } catch (IOException recovery) { e.addSuppressed(recovery); }
            }
            throw e;
        }
        if (moved) Files.delete(quarantine);
        return true;
    }

    public Path getStorageRoot() { return root; }

    private Path checkedPath(RmaAttachment attachment) throws IOException {
        Path path = Path.of(attachment.getStoragePath()).toAbsolutePath().normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IOException("Attachment is outside managed storage: " + path);
        }
        return path;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) return "";
        String ext = name.substring(dot);
        return ext.length() <= 16 && ext.matches("\\.[A-Za-z0-9]+") ? ext : "";
    }

    private static long lookupRmaId(String number) throws SQLException {
        try (java.sql.Connection c = DatabaseManager.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement("SELECT id FROM rmas WHERE rma_number = ?")) {
            ps.setString(1, number);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("RMA does not exist: " + number);
                return rs.getLong(1);
            }
        }
    }
}
