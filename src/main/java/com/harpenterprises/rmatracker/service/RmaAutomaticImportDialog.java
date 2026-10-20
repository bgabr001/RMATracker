package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.storage.DatabaseManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/** Compact, review-first importer. No attachment is written before explicit confirmation. */
public final class RmaAutomaticImportDialog extends JDialog {
    private final RmaAutomaticAttachmentService detector = new RmaAutomaticAttachmentService();
    private final RmaAttachmentStorageService storage = new RmaAttachmentStorageService();
    private final String defaultRma;
    private final DefaultListModel<Entry> files = new DefaultListModel<>();
    private final JList<Entry> fileList = new JList<>(files);
    private final JTextField destination = new JTextField(16);
    private final JLabel warning = new JLabel(" ");
    private final JLabel summary = new JLabel("Drop files or choose files to begin.");
    private final JButton browse = new JButton("Browse Files...");
    private final JButton detectedButton = new JButton("Use Detected RMA");
    private final JButton importButton = new JButton("Review Import");
    private boolean busy;
    private boolean updating;
    private final List<Path> temporaryFolders = new ArrayList<>();
    private static final long MAX_ZIP_FILE_BYTES = 100L * 1024 * 1024;
    private static final long MAX_ZIP_TOTAL_BYTES = 500L * 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 2000;

    private static final class Entry {
        final Path path;
        String detected;
        String target;
        String note;
        boolean imported;
        Long replaceId;
        Entry(Path path, String target) { this.path = path; this.target = target; }
        @Override public String toString() {
            String status = imported ? "Imported" : detected == null ? "Manual" : detected.equalsIgnoreCase(target) ? "Matched" : "Review";
            return path.getFileName() + "     → " + (target == null ? "Unassigned" : target) + "     [" + status + "]";
        }
    }

    public RmaAutomaticImportDialog(Window owner) { this(owner, null); }

    public RmaAutomaticImportDialog(Window owner, String currentRma) {
        super(owner, "Import RMA Documents", ModalityType.APPLICATION_MODAL);
        defaultRma = currentRma;
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(16, 18, 14, 18));
        setContentPane(root);
        JLabel title = new JLabel("Import RMA Documents");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        JLabel subtitle = new JLabel("Attach any file; PDFs are checked for RMA numbers.");
        subtitle.setForeground(Color.GRAY);
        JPanel headings = new JPanel(new GridLayout(2, 1, 0, 3));
        headings.add(title); headings.add(subtitle);
        root.add(headings, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(8, 10));
        JLabel drop = new JLabel("Drop files here  •  PDF, Word, Excel, TXT, images, ZIP and more", SwingConstants.CENTER);
        drop.setPreferredSize(new Dimension(570, 66));
        drop.setBorder(BorderFactory.createDashedBorder(new Color(140, 150, 164), 5, 5));
        drop.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport s) { return !busy && s.isDataFlavorSupported(DataFlavor.javaFileListFlavor); }
            @Override public boolean importData(TransferSupport s) {
                if (!canImport(s)) return false;
                try {
                    List<?> raw = (List<?>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    List<Path> paths = new ArrayList<>();
                    for (Object item : raw) if (item instanceof File f) paths.add(f.toPath());
                    analyzeFiles(paths);
                    return true;
                } catch (Exception ex) { showError(ex); return false; }
            }
        });
        middle.add(drop, BorderLayout.NORTH);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setVisibleRowCount(5);
        fileList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showSelection(); });
        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setPreferredSize(new Dimension(570, 135));
        middle.add(scroll, BorderLayout.CENTER);

        JPanel assignment = new JPanel(new BorderLayout(8, 7));
        assignment.setBorder(BorderFactory.createTitledBorder("Attachment destination — selected file"));
        JPanel editor = new JPanel(new BorderLayout(8, 0));
        editor.add(new JLabel("Assign to RMA:"), BorderLayout.WEST);
        editor.add(destination, BorderLayout.CENTER);
        editor.add(detectedButton, BorderLayout.EAST);
        assignment.add(editor, BorderLayout.NORTH);
        assignment.add(warning, BorderLayout.SOUTH);
        warning.setForeground(new Color(165, 95, 15));
        detectedButton.addActionListener(e -> {
            Entry selected = fileList.getSelectedValue();
            if (selected != null && selected.detected != null) destination.setText(selected.detected);
        });
        destination.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            private void changed() {
                if (updating) return;
                Entry selected = fileList.getSelectedValue();
                if (selected != null) { selected.target = destination.getText().trim(); fileList.repaint(); updateWarning(selected); }
            }
        });
        middle.add(assignment, BorderLayout.SOUTH);
        root.add(middle, BorderLayout.CENTER);

        browse.addActionListener(e -> selectFiles());
        importButton.addActionListener(e -> confirmAndImport());
        JButton close = new JButton("Close");
        close.addActionListener(e -> { if (!busy) dispose(); });
        JPanel footer = new JPanel(new BorderLayout(8, 6));
        footer.add(summary, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(browse); actions.add(importButton); actions.add(close);
        footer.add(actions, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
        importButton.setEnabled(false);
        setMinimumSize(new Dimension(640, 390));
        pack();
        setLocationRelativeTo(owner);
    }

    private void selectFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            analyzeFiles(Arrays.stream(chooser.getSelectedFiles()).map(File::toPath).toList());
    }

    public void analyzeFiles(List<Path> paths) {
        if (busy || paths == null || paths.isEmpty()) return;
        setBusy(true);
        summary.setText("Analyzing " + paths.size() + " file(s)...");
        new SwingWorker<List<Entry>, Void>() {
            @Override protected List<Entry> doInBackground() {
                List<Entry> results = new ArrayList<>();
                List<Path> expanded = new ArrayList<>();
                for (Path input : paths) {
                    try { expandInput(input, expanded); }
                    catch (IOException ex) {
                        Entry failed = new Entry(input.toAbsolutePath().normalize(), defaultRma);
                        failed.note = "Cannot open archive/folder: " + ex.getMessage();
                        results.add(failed);
                    }
                }
                for (Path path : expanded) {
                    Entry entry = new Entry(path.toAbsolutePath().normalize(), defaultRma);
                    if (!Files.isRegularFile(entry.path) || !Files.isReadable(entry.path)) {
                        entry.note = "File is not readable";
                    } else if (entry.path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                        try {
                            var preview = detector.preview(entry.path);
                            entry.detected = preview.rmaNumber();
                            if (entry.detected == null) {
                                var raw = new RmaPdfDetectionService().analyze(entry.path);
                                if (raw.pdfNumbers().size() == 1) entry.detected = raw.pdfNumbers().get(0);
                                else if (raw.filenameNumbers().size() == 1) entry.detected = raw.filenameNumbers().get(0);
                            }
                            if (preview.canImport()) entry.target = preview.rmaNumber();
                            entry.note = "PDF detection: " + preview.detectionStatus();
                        } catch (Exception ex) { entry.note = "PDF detection unavailable: " + ex.getMessage(); }
                    } else entry.note = "No PDF content detection; assign manually";
                    results.add(entry);
                }
                return results;
            }
            @Override protected void done() {
                try {
                    for (Entry entry : get()) files.addElement(entry);
                    if (!files.isEmpty()) fileList.setSelectedIndex(files.size() - 1);
                    summary.setText(files.size() + " file(s) ready for review. Assign each destination before importing.");
                } catch (Exception ex) { showError(ex); }
                finally { setBusy(false); }
            }
        }.execute();
    }

    private void showSelection() {
        Entry entry = fileList.getSelectedValue();
        updating = true;
        destination.setText(entry == null || entry.target == null ? "" : entry.target);
        updating = false;
        destination.setEnabled(entry != null && !busy);
        detectedButton.setEnabled(entry != null && entry.detected != null && !busy);
        updateWarning(entry);
    }

    private void updateWarning(Entry entry) {
        if (entry == null) { warning.setText("Select a file to assign it."); return; }
        if (entry.detected != null && !entry.detected.equalsIgnoreCase(entry.target == null ? "" : entry.target))
            warning.setText("Mismatch: PDF references " + entry.detected + ". Selected destination: " + entry.target);
        else if (entry.detected == null) warning.setText("No verified RMA detected. Select an existing RMA manually.");
        else warning.setText("Detected RMA matches the selected destination.");
        warning.setToolTipText(entry.note);
    }

    private static boolean exists(String rma) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM rmas WHERE rma_number = ?")) {
            ps.setString(1, rma);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void confirmAndImport() {
        if (busy || files.isEmpty()) return;
        List<Entry> pending = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> mismatches = new ArrayList<>();
        List<String> replacements = new ArrayList<>();
        try {
            for (int i = 0; i < files.size(); i++) {
                Entry entry = files.get(i);
                if (entry.imported) continue;
                if (!Files.isRegularFile(entry.path) || !Files.isReadable(entry.path)) {
                    errors.add(entry.path.getFileName() + ": file not readable"); continue;
                }
                if (entry.target == null || entry.target.isBlank() || !exists(entry.target)) {
                    errors.add(entry.path.getFileName() + ": select an existing RMA (currently " + entry.target + ")"); continue;
                }
                entry.replaceId = null;
                for (var existing : storage.list(entry.target)) {
                    if (existing.getOriginalFileName().equalsIgnoreCase(entry.path.getFileName().toString())) {
                        Object[] options = {"Keep Both", "Replace Existing", "Skip File", "Cancel Import"};
                        int choice = JOptionPane.showOptionDialog(this,
                                "RMA " + entry.target + " already contains a file named " + entry.path.getFileName() + ".",
                                "Existing attachment", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                                null, options, options[0]);
                        if (choice == 3 || choice == JOptionPane.CLOSED_OPTION) return;
                        if (choice == 2) { entry.note = "Skipped"; entry.imported = true; break; }
                        if (choice == 1) { entry.replaceId = existing.getId(); replacements.add(entry.path.getFileName() + " in " + entry.target); }
                        break;
                    }
                }
                if (entry.imported) continue;
                pending.add(entry);
                if (entry.detected != null && !entry.detected.equalsIgnoreCase(entry.target))
                    mismatches.add(entry.path.getFileName() + ": detected " + entry.detected + " → assigned " + entry.target);
            }
        } catch (SQLException ex) { showError(ex); return; }
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, String.join("\n", errors), "Resolve assignments", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (pending.isEmpty()) return;
        StringBuilder prompt = new StringBuilder("Import " + pending.size() + " file(s) into the selected RMAs?\n\n");
        for (Entry entry : pending) prompt.append(entry.path.getFileName()).append(" → ").append(entry.target).append('\n');
        if (!replacements.isEmpty()) {
            prompt.append("\nFiles to replace (existing copies will be removed):\n");
            replacements.forEach(s -> prompt.append(s).append('\n'));
        }
        if (!mismatches.isEmpty()) {
            prompt.append("\nWARNING: PDF RMA mismatches (manual override):\n");
            mismatches.forEach(s -> prompt.append(s).append('\n'));
        }
        JTextArea review = new JTextArea(prompt.toString(), Math.min(16, pending.size() + mismatches.size() + 5), 58);
        review.setEditable(false); review.setCaretPosition(0);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(review), "Confirm attachment destinations",
                JOptionPane.YES_NO_OPTION, mismatches.isEmpty() ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) return;
        setBusy(true);
        summary.setText("Importing " + pending.size() + " file(s)...");
        new SwingWorker<int[], Void>() {
            @Override protected int[] doInBackground() {
                int[] counts = new int[3];
                for (Entry entry : pending) {
                    try {
                        {
                            // The user explicitly chose to import, including identical copies.
                            // Save first so replacement never deletes the only existing copy.
                            var added = storage.add(entry.target, entry.path, true);
                            if (entry.replaceId != null) {
                                try { storage.remove(entry.replaceId); }
                                catch (Exception ex) {
                                    // Keep both copies if cleanup fails; never discard the new attachment.
                                    entry.note = "Imported; old copy could not be removed: " + ex.getMessage();
                                }
                            }
                            counts[0]++;
                            if (entry.note == null || !entry.note.startsWith("Imported;")) entry.note = "Imported";
                        }
                        entry.imported = true;
                    } catch (Exception ex) { counts[2]++; entry.note = "Error: " + ex.getMessage(); }
                }
                return counts;
            }
            @Override protected void done() {
                try {
                    int[] counts = get();
                    summary.setText("Imported: " + counts[0] + "  •  Duplicates: " + counts[1] + "  •  Errors: " + counts[2]);
                    fileList.repaint();
                    if (counts[2] > 0) {
                        StringBuilder failures = new StringBuilder();
                        for (Entry e : pending) if (e.note != null && e.note.startsWith("Error:"))
                            failures.append(e.path.getFileName()).append(": ").append(e.note).append('\n');
                        JOptionPane.showMessageDialog(RmaAutomaticImportDialog.this, failures.toString(), "Import errors", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) { showError(ex); }
                finally { setBusy(false); showSelection(); }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        busy = value;
        browse.setEnabled(!value);
        importButton.setEnabled(!value && !files.isEmpty());
        destination.setEnabled(!value && fileList.getSelectedValue() != null);
        detectedButton.setEnabled(!value && fileList.getSelectedValue() != null && fileList.getSelectedValue().detected != null);
        setDefaultCloseOperation(value ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
    }

    private void expandInput(Path input, List<Path> output) throws IOException {
        Path path = input.toAbsolutePath().normalize();
        if (isIgnoredPath(path)) return;
        if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
                for (Path child : stream.filter(Files::isRegularFile).toList()) expandInput(child, output);
            }
        } else if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            Path temp = Files.createTempDirectory("rma-zip-import-");
            synchronized (temporaryFolders) { temporaryFolders.add(temp); }
            long total = 0;
            int entries = 0;
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(path))) {
                ZipEntry item;
                while ((item = zip.getNextEntry()) != null) {
                    if (++entries > MAX_ZIP_ENTRIES) throw new IOException("ZIP contains too many entries");
                    Path target = temp.resolve(item.getName()).normalize();
                    if (!target.startsWith(temp)) throw new IOException("Unsafe ZIP entry: " + item.getName());
                    // Ignore dotfiles, dot-directories, and macOS archive metadata.
                    // Check the archive-relative path, not the temporary folder name.
                    Path relative = temp.relativize(target);
                    if (isIgnoredPath(relative)) { zip.closeEntry(); continue; }
                    if (item.isDirectory()) { Files.createDirectories(target); continue; }
                    Files.createDirectories(target.getParent());
                    long fileBytes = 0;
                    try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            fileBytes += read;
                            total += read;
                            if (fileBytes > MAX_ZIP_FILE_BYTES || total > MAX_ZIP_TOTAL_BYTES)
                                throw new IOException("ZIP extraction size limit exceeded");
                            out.write(buffer, 0, read);
                        }
                    }
                    output.add(target);
                    zip.closeEntry();
                }
            }
        } else output.add(path);
    }

    /** Exclude dot-prefixed files/folders and Finder ZIP metadata at any depth. */
    private static boolean isIgnoredPath(Path path) {
        for (Path component : path) {
            String name = component.toString();
            if (name.startsWith(".") || name.equalsIgnoreCase("__MACOSX")) return true;
        }
        return false;
    }

    @Override public void dispose() {
        synchronized (temporaryFolders) {
            for (Path folder : temporaryFolders) {
                try (var paths = Files.walk(folder)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                } catch (IOException ignored) { /* Best effort temporary file cleanup. */ }
            }
            temporaryFolders.clear();
        }
        super.dispose();
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, String.valueOf(ex.getMessage()), "Document import error", JOptionPane.ERROR_MESSAGE);
    }
}
