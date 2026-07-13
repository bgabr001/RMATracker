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

        try (Connection connection =
                     DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, rmaNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    historyEntries.add(
                            mapStatusHistory(resultSet)
                    );
                }
            }
        }

        return historyEntries;
    }

    private StatusHistory mapStatusHistory(
            ResultSet resultSet
    ) throws SQLException {

        String oldStatusText =
                resultSet.getString("old_status");

        Status oldStatus =
                oldStatusText == null
                        ? null
                        : Status.valueOf(oldStatusText);

        Status newStatus = Status.valueOf(
                resultSet.getString("new_status")
        );

        LocalDateTime changedAt = parseDateTime(
                resultSet.getString("changed_at")
        );

        return new StatusHistory(
                resultSet.getLong("id"),
                resultSet.getString("rma_number"),
                oldStatus,
                newStatus,
                changedAt
        );
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
                    dateTimeText.trim().replace(' ', 'T')
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
