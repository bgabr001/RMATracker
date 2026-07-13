package com.harpenterprises.rmatracker.storage;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RmaRepository {

    public void save(RmaRecord rmaRecord) throws SQLException {
        String sql = """
                INSERT INTO rmas (
                    rma_number,
                    date_sent,
                    date_received,
                    status,
                    outgoing_tracking_number,
                    return_tracking_number,
                    notes
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection =
                     DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {
                long rmaId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     sql,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    setRmaInsertValues(statement, rmaRecord);

                    int rowsInserted = statement.executeUpdate();

                    if (rowsInserted == 0) {
                        throw new SQLException(
                                "The RMA could not be saved."
                        );
                    }

                    try (ResultSet generatedKeys =
                                 statement.getGeneratedKeys()) {

                        if (!generatedKeys.next()) {
                            throw new SQLException(
                                    "The generated RMA ID "
                                            + "could not be retrieved."
                            );
                        }

                        rmaId = generatedKeys.getLong(1);
                    }
                }

                insertRepairItems(
                        connection,
                        rmaId,
                        rmaRecord.getRepairItems()
                );

                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void update(RmaRecord rmaRecord) throws SQLException {
        String sql = """
                UPDATE rmas
                SET date_sent = ?,
                    date_received = ?,
                    status = ?,
                    outgoing_tracking_number = ?,
                    return_tracking_number = ?,
                    notes = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE rma_number = ?
                """;

        try (Connection connection =
                     DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {
                long rmaId = findRmaId(
                        connection,
                        rmaRecord.getRmaNumber()
                );

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    setNullableDate(
                            statement,
                            1,
                            rmaRecord.getDateSent()
                    );

                    setNullableDate(
                            statement,
                            2,
                            rmaRecord.getDateReceived()
                    );

                    statement.setString(
                            3,
                            rmaRecord.getStatus().name()
                    );

                    setNullableString(
                            statement,
                            4,
                            rmaRecord.getOutgoingTrackingNumber()
                    );

                    setNullableString(
                            statement,
                            5,
                            rmaRecord.getReturnTrackingNumber()
                    );

                    setNullableString(
                            statement,
                            6,
                            rmaRecord.getNotes()
                    );

                    statement.setString(
                            7,
                            rmaRecord.getRmaNumber()
                    );

                    int rowsUpdated = statement.executeUpdate();

                    if (rowsUpdated == 0) {
                        throw new SQLException(
                                "No RMA was found with number: "
                                        + rmaRecord.getRmaNumber()
                        );
                    }
                }

                deleteRepairItems(connection, rmaId);

                insertRepairItems(
                        connection,
                        rmaId,
                        rmaRecord.getRepairItems()
                );

                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean delete(String rmaNumber) throws SQLException {
        String sql = """
                DELETE FROM rmas
                WHERE rma_number = ?
                """;

        try (Connection connection =
                     DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, rmaNumber);

            return statement.executeUpdate() > 0;
        }
    }

    public Optional<RmaRecord> findByRmaNumber(
            String rmaNumber
    ) throws SQLException {

        String sql = """
                SELECT *
                FROM rmas
                WHERE rma_number = ?
                """;

        try (Connection connection =
                     DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, rmaNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapRmaRecord(connection, resultSet)
                );
            }
        }
    }

    public List<RmaRecord> findAll() throws SQLException {
        String sql = """
                SELECT *
                FROM rmas
                ORDER BY
                    CASE
                        WHEN date_sent IS NULL THEN 1
                        ELSE 0
                    END,
                    date_sent DESC,
                    rma_number ASC
                """;

        List<RmaRecord> records = new ArrayList<>();

        try (Connection connection =
                     DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {
                records.add(
                        mapRmaRecord(connection, resultSet)
                );
            }
        }

        return records;
    }

    public List<RmaRecord> search(String searchText)
            throws SQLException {

        if (searchText == null || searchText.isBlank()) {
            return findAll();
        }

        String sql = """
                SELECT DISTINCT r.*
                FROM rmas r
                LEFT JOIN repair_items i
                    ON r.id = i.rma_id
                WHERE LOWER(r.rma_number) LIKE ?
                   OR LOWER(r.status) LIKE ?
                   OR LOWER(COALESCE(
                       r.outgoing_tracking_number, ''
                   )) LIKE ?
                   OR LOWER(COALESCE(
                       r.return_tracking_number, ''
                   )) LIKE ?
                   OR LOWER(COALESCE(r.notes, '')) LIKE ?
                   OR LOWER(COALESCE(i.county, '')) LIKE ?
                   OR LOWER(COALESCE(
                       i.machine_type, ''
                   )) LIKE ?
                   OR LOWER(COALESCE(
                       i.serial_number, ''
                   )) LIKE ?
                   OR LOWER(COALESCE(i.version, '')) LIKE ?
                   OR LOWER(COALESCE(
                       i.problem_description, ''
                   )) LIKE ?
                   OR LOWER(COALESCE(
                       i.repair_description, ''
                   )) LIKE ?
                ORDER BY
                    r.date_sent DESC,
                    r.rma_number ASC
                """;

        String searchPattern =
                "%"
                        + searchText.trim().toLowerCase()
                        + "%";

        List<RmaRecord> records = new ArrayList<>();

        try (Connection connection =
                     DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (int index = 1; index <= 11; index++) {
                statement.setString(index, searchPattern);
            }

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    records.add(
                            mapRmaRecord(connection, resultSet)
                    );
                }
            }
        }

        return records;
    }

    public boolean existsByRmaNumber(String rmaNumber)
            throws SQLException {

        String sql = """
                SELECT 1
                FROM rmas
                WHERE rma_number = ?
                LIMIT 1
                """;

        try (Connection connection =
                     DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, rmaNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next();
            }
        }
    }

    private void insertRepairItems(
            Connection connection,
            long rmaId,
            List<RepairItem> repairItems
    ) throws SQLException {

        if (repairItems == null || repairItems.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO repair_items (
                    rma_id,
                    county,
                    machine_type,
                    serial_number,
                    version,
                    problem_description,
                    repair_description
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (RepairItem item : repairItems) {
                statement.setLong(1, rmaId);

                setNullableString(
                        statement,
                        2,
                        item.getCounty()
                );

                setNullableString(
                        statement,
                        3,
                        item.getMachineType()
                );

                setNullableString(
                        statement,
                        4,
                        item.getSerialNumber()
                );

                setNullableString(
                        statement,
                        5,
                        item.getVersion()
                );

                setNullableString(
                        statement,
                        6,
                        item.getProblemDescription()
                );

                setNullableString(
                        statement,
                        7,
                        item.getRepairDescription()
                );

                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private void deleteRepairItems(
            Connection connection,
            long rmaId
    ) throws SQLException {

        String sql = """
                DELETE FROM repair_items
                WHERE rma_id = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, rmaId);
            statement.executeUpdate();
        }
    }

    private long findRmaId(
            Connection connection,
            String rmaNumber
    ) throws SQLException {

        String sql = """
                SELECT id
                FROM rmas
                WHERE rma_number = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, rmaNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new SQLException(
                            "No RMA was found with number: "
                                    + rmaNumber
                    );
                }

                return resultSet.getLong("id");
            }
        }
    }

    private RmaRecord mapRmaRecord(
            Connection connection,
            ResultSet resultSet
    ) throws SQLException {

        long rmaId = resultSet.getLong("id");

        String rmaNumber =
                resultSet.getString("rma_number");

        LocalDate dateSent =
                getNullableDate(resultSet, "date_sent");

        LocalDate dateReceived =
                getNullableDate(
                        resultSet,
                        "date_received"
                );

        Status status = Status.valueOf(
                resultSet.getString("status")
        );

        String outgoingTrackingNumber =
                resultSet.getString(
                        "outgoing_tracking_number"
                );

        String returnTrackingNumber =
                resultSet.getString(
                        "return_tracking_number"
                );

        String notes =
                resultSet.getString("notes");

        List<RepairItem> repairItems =
                findRepairItems(connection, rmaId);

        return new RmaRecord(
                rmaNumber,
                dateSent,
                dateReceived,
                status,
                outgoingTrackingNumber,
                returnTrackingNumber,
                notes,
                repairItems
        );
    }

    private List<RepairItem> findRepairItems(
            Connection connection,
            long rmaId
    ) throws SQLException {

        String sql = """
                SELECT *
                FROM repair_items
                WHERE rma_id = ?
                ORDER BY id ASC
                """;

        List<RepairItem> repairItems =
                new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, rmaId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    RepairItem item = new RepairItem(
                            resultSet.getString("county"),
                            resultSet.getString(
                                    "machine_type"
                            ),
                            resultSet.getString(
                                    "serial_number"
                            ),
                            resultSet.getString("version"),
                            resultSet.getString(
                                    "problem_description"
                            ),
                            resultSet.getString(
                                    "repair_description"
                            )
                    );

                    repairItems.add(item);
                }
            }
        }

        return repairItems;
    }

    private void setRmaInsertValues(
            PreparedStatement statement,
            RmaRecord rmaRecord
    ) throws SQLException {

        statement.setString(
                1,
                rmaRecord.getRmaNumber()
        );

        setNullableDate(
                statement,
                2,
                rmaRecord.getDateSent()
        );

        setNullableDate(
                statement,
                3,
                rmaRecord.getDateReceived()
        );

        statement.setString(
                4,
                rmaRecord.getStatus().name()
        );

        setNullableString(
                statement,
                5,
                rmaRecord.getOutgoingTrackingNumber()
        );

        setNullableString(
                statement,
                6,
                rmaRecord.getReturnTrackingNumber()
        );

        setNullableString(
                statement,
                7,
                rmaRecord.getNotes()
        );
    }

    private void setNullableDate(
            PreparedStatement statement,
            int parameterIndex,
            LocalDate date
    ) throws SQLException {

        if (date == null) {
            statement.setNull(
                    parameterIndex,
                    Types.VARCHAR
            );
        } else {
            statement.setString(
                    parameterIndex,
                    date.toString()
            );
        }
    }

    private void setNullableString(
            PreparedStatement statement,
            int parameterIndex,
            String value
    ) throws SQLException {

        if (value == null || value.isBlank()) {
            statement.setNull(
                    parameterIndex,
                    Types.VARCHAR
            );
        } else {
            statement.setString(
                    parameterIndex,
                    value.trim()
            );
        }
    }

    private LocalDate getNullableDate(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {

        String dateText =
                resultSet.getString(columnName);

        if (dateText == null || dateText.isBlank()) {
            return null;
        }

        return LocalDate.parse(dateText);
    }
}