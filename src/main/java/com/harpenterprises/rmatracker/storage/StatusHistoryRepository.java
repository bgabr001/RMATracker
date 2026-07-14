package com.harpenterprises.rmatracker.storage;

import com.harpenterprises.rmatracker.model.Status;
import com.harpenterprises.rmatracker.model.StatusHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatusHistoryRepository {

    public List<StatusHistory> findByRmaNumber(
            String rmaNumber
    ) throws SQLException {

        String sql = """
                SELECT
                    h.id,
                    r.rma_number,
                    h.old_status,
                    h.new_status,
                    h.changed_at
                FROM status_history h
                INNER JOIN rmas r
                    ON r.id = h.rma_id
                WHERE r.rma_number = ?
                ORDER BY h.changed_at ASC, h.id ASC
                """;

        List<StatusHistory> historyEntries =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, rmaNumber);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                while (resultSet.next()) {
                    historyEntries.add(
                            mapStatusHistory(resultSet)
                    );
                }
            }
        }

        return historyEntries;
    }

    /**
     * Deletes one status-history row by its database ID.
     *
     * @return true when one row was deleted; otherwise false
     */
    public boolean deleteById(long historyId)
            throws SQLException {

        String sql = """
                DELETE FROM status_history
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, historyId);
            return statement.executeUpdate() == 1;
        }
    }

    private StatusHistory mapStatusHistory(
            ResultSet resultSet
    ) throws SQLException {

        String oldStatusText =
                resultSet.getString("old_status");

        String newStatusText =
                resultSet.getString("new_status");

        Status oldStatus =
                oldStatusText == null
                        || oldStatusText.isBlank()
                        ? null
                        : parseStatus(oldStatusText);

        Status newStatus =
                parseStatus(newStatusText);

        LocalDateTime changedAt =
                parseDateTime(
                        resultSet.getString(
                                "changed_at"
                        )
                );

        return new StatusHistory(
                resultSet.getLong("id"),
                resultSet.getString("rma_number"),
                oldStatus,
                newStatus,
                changedAt
        );
    }

    private Status parseStatus(
            String statusText
    ) throws SQLException {

        if (statusText == null
                || statusText.isBlank()) {

            throw new SQLException(
                    "A status-history status was missing."
            );
        }

        String normalizedStatus =
                statusText
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        .replace(' ', '_')
                        .replace('-', '_');

        /*
         * Convert older database status names
         * into the current Status enum values.
         */
        switch (normalizedStatus) {
            case "RETURNED":
                return Status.RECEIVED_BACK;

            case "READY":
                return Status.READY_TO_SHIP;

            case "REPAIRING":
                return Status.IN_REPAIR;

            case "COMPLETE":
                return Status.CLOSED;

            default:
                break;
        }

        try {
            return Status.valueOf(
                    normalizedStatus
            );

        } catch (IllegalArgumentException exception) {
            throw new SQLException(
                    "Unknown status value in status history: "
                            + statusText,
                    exception
            );
        }
    }

    private LocalDateTime parseDateTime(
            String dateTimeText
    ) throws SQLException {

        if (dateTimeText == null
                || dateTimeText.isBlank()) {

            throw new SQLException(
                    "A status-history date was missing."
            );
        }

        try {
            return LocalDateTime.parse(
                    dateTimeText
                            .trim()
                            .replace(' ', 'T')
            );

        } catch (RuntimeException exception) {
            throw new SQLException(
                    "Could not read status-history date: "
                            + dateTimeText,
                    exception
            );
        }
    }
}
