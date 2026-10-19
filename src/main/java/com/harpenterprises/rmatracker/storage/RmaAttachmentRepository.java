package com.harpenterprises.rmatracker.storage;

import com.harpenterprises.rmatracker.model.RmaAttachment;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Stores attachment metadata only; actual file copying is handled by Step 7C. */
public class RmaAttachmentRepository {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public long save(RmaAttachment attachment) throws SQLException {
        Objects.requireNonNull(attachment, "attachment");
        String sql = """
                INSERT INTO rma_attachments
                (rma_id, original_file_name, stored_file_name, storage_path,
                 file_size, content_type, date_added)
                VALUES ((SELECT id FROM rmas WHERE rma_number = ?), ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, required(attachment.getRmaNumber(), "RMA number"));
            statement.setString(2, required(attachment.getOriginalFileName(), "Original filename"));
            statement.setString(3, required(attachment.getStoredFileName(), "Stored filename"));
            statement.setString(4, required(attachment.getStoragePath(), "Storage path"));
            if (attachment.getFileSize() < 0) throw new IllegalArgumentException("File size cannot be negative");
            statement.setLong(5, attachment.getFileSize());
            statement.setString(6, attachment.getContentType());
            LocalDateTime date = attachment.getDateAdded() == null ? LocalDateTime.now() : attachment.getDateAdded();
            statement.setString(7, date.format(FORMAT));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Attachment ID was not returned");
                long id = keys.getLong(1);
                attachment.setId(id);
                attachment.setDateAdded(date);
                return id;
            }
        }
    }

    public List<RmaAttachment> findByRmaNumber(String rmaNumber) throws SQLException {
        String sql = """
                SELECT a.*, r.rma_number FROM rma_attachments a
                JOIN rmas r ON r.id = a.rma_id
                WHERE r.rma_number = ? ORDER BY a.date_added DESC, a.id DESC
                """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, required(rmaNumber, "RMA number"));
            try (ResultSet rs = ps.executeQuery()) {
                List<RmaAttachment> attachments = new ArrayList<>();
                while (rs.next()) attachments.add(map(rs));
                return attachments;
            }
        }
    }

    public RmaAttachment findById(long id) throws SQLException {
        String sql = """
                SELECT a.*, r.rma_number FROM rma_attachments a
                JOIN rmas r ON r.id = a.rma_id WHERE a.id = ?
                """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Removes metadata only. Step 7C will coordinate deletion of the file itself. */
    public boolean deleteById(long id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM rma_attachments WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static RmaAttachment map(ResultSet rs) throws SQLException {
        return new RmaAttachment(
                rs.getLong("id"), rs.getString("rma_number"),
                rs.getString("original_file_name"), rs.getString("stored_file_name"),
                rs.getString("storage_path"), rs.getLong("file_size"),
                rs.getString("content_type"), LocalDateTime.parse(rs.getString("date_added").replace('T', ' '), FORMAT));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
