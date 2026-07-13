package com.harpenterprises.rmatracker.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private static final String DATABASE_FOLDER = "data";
    private static final String DATABASE_FILE = "rma-tracker.db";

    private static final String DATABASE_URL =
            "jdbc:sqlite:"
                    + DATABASE_FOLDER
                    + File.separator
                    + DATABASE_FILE;

    private DatabaseManager() {
        // Prevent this utility class from being instantiated.
    }

    /**
     * Returns the location of the active SQLite database file.
     */
    public static File getDatabaseFile() {
        createDatabaseFolder();
        return new File(DATABASE_FOLDER, DATABASE_FILE);
    }

    public static Connection getConnection() throws SQLException {
        createDatabaseFolder();

        Connection connection =
                DriverManager.getConnection(DATABASE_URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }

    public static void initializeDatabase() {
        createDatabaseFolder();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS rmas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        rma_number TEXT NOT NULL UNIQUE,
                        date_sent TEXT,
                        date_received TEXT,
                        status TEXT NOT NULL,
                        outgoing_tracking_number TEXT,
                        return_tracking_number TEXT,
                        notes TEXT,
                        created_at TEXT NOT NULL
                            DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT NOT NULL
                            DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS repair_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        rma_id INTEGER NOT NULL,
                        county TEXT NOT NULL,
                        machine_type TEXT NOT NULL,
                        serial_number TEXT,
                        version TEXT,
                        problem_description TEXT,
                        repair_description TEXT,
                        FOREIGN KEY (rma_id)
                            REFERENCES rmas(id)
                            ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS status_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        rma_id INTEGER NOT NULL,
                        old_status TEXT,
                        new_status TEXT NOT NULL,
                        changed_at TEXT NOT NULL,
                        FOREIGN KEY (rma_id)
                            REFERENCES rmas(id)
                            ON DELETE CASCADE
                    )
                    """);

            /*
             * Give RMAs created before status history existed
             * one initial history entry using their current status.
             */
            statement.execute("""
                    INSERT INTO status_history (
                        rma_id,
                        old_status,
                        new_status,
                        changed_at
                    )
                    SELECT
                        r.id,
                        NULL,
                        r.status,
                        COALESCE(r.created_at, CURRENT_TIMESTAMP)
                    FROM rmas r
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM status_history h
                        WHERE h.rma_id = r.id
                    )
                    """);

            /*
             * This handles databases that were created before the
             * version column was added.
             */
            addColumnIfMissing(
                    connection,
                    "repair_items",
                    "version",
                    "TEXT"
            );

            addColumnIfMissing(
                    connection,
                    "repair_items",
                    "repair_description",
                    "TEXT"
            );

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_rmas_rma_number
                    ON rmas(rma_number)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_items_rma_id
                    ON repair_items(rma_id)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_items_county
                    ON repair_items(county)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_items_serial
                    ON repair_items(serial_number)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_history_rma_id
                    ON status_history(rma_id)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_history_changed_at
                    ON status_history(changed_at)
                    """);

            System.out.println(
                    "SQLite database initialized successfully."
            );

            System.out.println(
                    "Database location: "
                            + new File(
                            DATABASE_FOLDER,
                            DATABASE_FILE
                    ).getAbsolutePath()
            );

        } catch (SQLException exception) {
            System.err.println(
                    "Could not initialize the SQLite database."
            );

            exception.printStackTrace();
        }
    }

    private static void addColumnIfMissing(
            Connection connection,
            String tableName,
            String columnName,
            String columnDefinition
    ) throws SQLException {

        if (columnExists(connection, tableName, columnName)) {
            return;
        }

        String sql =
                "ALTER TABLE "
                        + tableName
                        + " ADD COLUMN "
                        + columnName
                        + " "
                        + columnDefinition;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }

        System.out.println(
                "Added database column: "
                        + tableName
                        + "."
                        + columnName
        );
    }

    private static boolean columnExists(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {

        String sql = "PRAGMA table_info(" + tableName + ")";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String existingColumn =
                        resultSet.getString("name");

                if (columnName.equalsIgnoreCase(existingColumn)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void createDatabaseFolder() {
        File folder = new File(DATABASE_FOLDER);

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create the database folder: "
                            + folder.getAbsolutePath()
            );
        }
    }
}