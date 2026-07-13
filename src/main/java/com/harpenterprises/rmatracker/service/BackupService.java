package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.storage.DatabaseManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Creates and restores backups of the RMA Tracker SQLite database.
 */
public class BackupService {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private static final byte[] SQLITE_HEADER =
            "SQLite format 3\000".getBytes(StandardCharsets.US_ASCII);

    /**
     * Creates a timestamped copy of the active database in the
     * selected destination folder.
     */
    public Path createBackup(Path destinationFolder)
            throws IOException, SQLException {

        if (destinationFolder == null) {
            throw new IllegalArgumentException(
                    "A backup destination folder is required."
            );
        }

        Files.createDirectories(destinationFolder);

        Path databasePath = getDatabasePath();

        if (!Files.isRegularFile(databasePath)) {
            throw new IOException(
                    "The active database file could not be found: "
                            + databasePath.toAbsolutePath()
            );
        }

        checkpointDatabase();

        String timestamp = LocalDateTime.now()
                .format(FILE_TIMESTAMP);

        Path backupPath = destinationFolder.resolve(
                "rma-tracker-backup-" + timestamp + ".db"
        );

        return Files.copy(
                databasePath,
                backupPath,
                StandardCopyOption.COPY_ATTRIBUTES
        );
    }

    /**
     * Restores the selected database backup. Before replacement, a
     * timestamped safety backup of the current database is created.
     *
     * @return the path of the automatic safety backup
     */
    public Path restoreBackup(Path selectedBackup)
            throws IOException, SQLException {

        validateBackup(selectedBackup);

        Path databasePath = getDatabasePath();
        Path databaseFolder = databasePath.getParent();

        if (databaseFolder == null) {
            throw new IOException(
                    "The database folder could not be determined."
            );
        }

        Files.createDirectories(databaseFolder);

        Path safetyFolder = databaseFolder.resolve("restore-safety-backups");
        Path safetyBackup = null;

        if (Files.isRegularFile(databasePath)) {
            safetyBackup = createBackup(safetyFolder);
        }

        Path temporaryRestore = databaseFolder.resolve(
                "rma-tracker.restore.tmp"
        );

        try {
            Files.copy(
                    selectedBackup,
                    temporaryRestore,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
            );

            Files.move(
                    temporaryRestore,
                    databasePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            deleteSQLiteSidecarFiles(databasePath);
            DatabaseManager.initializeDatabase();

            return safetyBackup;

        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporaryRestore);
            throw exception;
        }
    }

    /**
     * Verifies that a selected file is a readable SQLite database and
     * contains the tables required by RMA Tracker.
     */
    public void validateBackup(Path backupPath)
            throws IOException, SQLException {

        if (backupPath == null || !Files.isRegularFile(backupPath)) {
            throw new IOException(
                    "Please select an existing database backup file."
            );
        }

        if (Files.size(backupPath) < SQLITE_HEADER.length) {
            throw new IOException(
                    "The selected file is too small to be a SQLite database."
            );
        }

        byte[] actualHeader = new byte[SQLITE_HEADER.length];

        try (var input = Files.newInputStream(backupPath)) {
            int bytesRead = input.read(actualHeader);

            if (bytesRead != SQLITE_HEADER.length) {
                throw new IOException(
                        "The selected file could not be read."
                );
            }
        }

        for (int index = 0; index < SQLITE_HEADER.length; index++) {
            if (actualHeader[index] != SQLITE_HEADER[index]) {
                throw new IOException(
                        "The selected file is not a valid SQLite database."
                );
            }
        }

        String url = "jdbc:sqlite:" + backupPath.toAbsolutePath();

        try (Connection connection = DriverManager.getConnection(url)) {
            verifyRequiredTable(connection, "rmas");
            verifyRequiredTable(connection, "repair_items");
            verifyRequiredTable(connection, "status_history");

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "PRAGMA integrity_check"
                 )) {

                if (!resultSet.next()
                        || !"ok".equalsIgnoreCase(resultSet.getString(1))) {

                    throw new SQLException(
                            "The selected SQLite database failed its integrity check."
                    );
                }
            }
        }
    }

    private void checkpointDatabase() throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA wal_checkpoint(FULL)");
        }
    }

    private void verifyRequiredTable(
            Connection connection,
            String tableName
    ) throws SQLException {

        String sql = """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table'
                  AND name = '%s'
                """.formatted(tableName);

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                throw new SQLException(
                        "The selected database is missing the required table: "
                                + tableName
                );
            }
        }
    }

    private Path getDatabasePath() {
        return DatabaseManager
                .getDatabaseFile()
                .toPath()
                .toAbsolutePath()
                .normalize();
    }

    private void deleteSQLiteSidecarFiles(Path databasePath)
            throws IOException {

        Files.deleteIfExists(Path.of(databasePath + "-wal"));
        Files.deleteIfExists(Path.of(databasePath + "-shm"));
        Files.deleteIfExists(Path.of(databasePath + "-journal"));
    }
}
