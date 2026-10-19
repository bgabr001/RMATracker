package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.storage.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;
import java.util.Comparator;
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

        Path backupPath = uniqueBackupPath(destinationFolder, timestamp);
        Path temporary = Files.createTempFile(destinationFolder, "rma-backup-", ".tmp");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                addZipFile(zip, databasePath, "database/rma-tracker.db");
                Path attachments = getAttachmentRoot();
                if (Files.isDirectory(attachments)) {
                    try (Stream<Path> files = Files.walk(attachments)) {
                        for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                            if (Files.isSymbolicLink(file)) {
                                throw new IOException("Attachment symlink cannot be backed up: " + file);
                            }
                            addZipFile(zip, file, "attachments/" + attachments.relativize(file).toString().replace('\\', '/'));
                        }
                    }
                }
            }
            return Files.move(temporary, backupPath);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * Restores the selected database backup. Before replacement, a
     * timestamped safety backup of the current database is created.
     *
     * @return the path of the automatic safety backup
     */
    public Path restoreBackup(Path selectedBackup)
            throws IOException, SQLException {

        if (selectedBackup == null || !Files.isRegularFile(selectedBackup)) {
            throw new IOException("Please select an existing backup file.");
        }
        Path databasePath = getDatabasePath();
        Path databaseFolder = databasePath.getParent();
        Path attachments = getAttachmentRoot();
        Files.createDirectories(databaseFolder);
        Path staging = Files.createTempDirectory(databaseFolder, "rma-restore-");
        try {
            Path stagedDatabase;
            boolean completeBackup = isZip(selectedBackup);
            if (completeBackup) {
                extractBackup(selectedBackup, staging);
                stagedDatabase = staging.resolve("database/rma-tracker.db");
            } else {
                stagedDatabase = selectedBackup;
            }
            validateSqlite(stagedDatabase);
            // Safety backup is always complete, including attachments.
            Path safetyBackup = null;
            if (Files.isRegularFile(databasePath)) {
                safetyBackup = createBackup(databaseFolder.resolve("restore-safety-backups"));
            }
            Path replacement = Files.createTempFile(databaseFolder, "rma-db-", ".tmp");
            try {
                Files.copy(stagedDatabase, replacement, StandardCopyOption.REPLACE_EXISTING);
                // Replace the attachments directory only for a complete ZIP backup.
                // A legacy .db backup cannot restore attachment bytes.
                if (completeBackup) {
                    Path incoming = staging.resolve("attachments");
                    Files.createDirectories(incoming);
                    Path oldAttachments = staging.resolve("previous-attachments");
                    boolean hadAttachments = Files.exists(attachments);
                    if (hadAttachments) Files.move(attachments, oldAttachments);
                    try {
                        Files.move(incoming, attachments);
                        try {
                            Files.move(replacement, databasePath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            deleteTree(attachments);
                            if (hadAttachments) Files.move(oldAttachments, attachments);
                            throw ex;
                        }
                    } catch (IOException ex) {
                        if (hadAttachments && !Files.exists(attachments) && Files.exists(oldAttachments)) {
                            Files.move(oldAttachments, attachments);
                        }
                        throw ex;
                    }
                } else {
                    Files.move(replacement, databasePath, StandardCopyOption.REPLACE_EXISTING);
                }
                deleteSQLiteSidecarFiles(databasePath);
                DatabaseManager.initializeDatabase();
                return safetyBackup;
            } finally {
                Files.deleteIfExists(replacement);
            }
        } finally {
            deleteTree(staging);
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

        if (isZip(backupPath)) {
            Path temp = Files.createTempDirectory("rma-validate-");
            try {
                extractBackup(backupPath, temp);
                validateSqlite(temp.resolve("database/rma-tracker.db"));
            } finally {
                deleteTree(temp);
            }
            return;
        }
        validateSqlite(backupPath);
    }

    private void validateSqlite(Path backupPath) throws IOException, SQLException {
        if (!Files.isRegularFile(backupPath)) throw new IOException("Backup is missing its database file.");
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

    private Path getAttachmentRoot() {
        return DatabaseManager.getApplicationFolder().toPath().resolve("attachments").toAbsolutePath().normalize();
    }

    private Path uniqueBackupPath(Path folder, String timestamp) {
        Path path = folder.resolve("rma-tracker-backup-" + timestamp + ".zip");
        int number = 2;
        while (Files.exists(path)) path = folder.resolve("rma-tracker-backup-" + timestamp + "-" + number++ + ".zip");
        return path;
    }

    private void addZipFile(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private boolean isZip(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return input.read() == 'P' && input.read() == 'K';
        }
    }

    private void extractBackup(Path zipPath, Path staging) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!(name.equals("database/rma-tracker.db") || name.startsWith("attachments/"))) {
                    throw new IOException("Unexpected entry in backup: " + name);
                }
                Path target = staging.resolve(name).normalize();
                if (!target.startsWith(staging)) throw new IOException("Unsafe ZIP path: " + name);
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    if (Files.exists(target)) throw new IOException("Duplicate ZIP entry: " + name);
                    Files.copy(zip, target);
                }
                zip.closeEntry();
            }
        }
    }

    private void deleteTree(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
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
