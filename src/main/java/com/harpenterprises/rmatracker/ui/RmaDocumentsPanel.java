package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RmaAttachment;
import com.harpenterprises.rmatracker.service.RmaAttachmentStorageService;

import com.harpenterprises.rmatracker.service.RmaAutomaticImportDialog;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import javax.swing.border.Border;
import java.io.IOException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Step 7E: RMA attachment list and file actions. */
public class RmaDocumentsPanel extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final String rmaNumber;
    private final RmaAttachmentStorageService storage = new RmaAttachmentStorageService();
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel message = new JLabel(" ");
    private final JButton addButton = new JButton("Add Document");
    private final JButton autoImportButton = new JButton("Import & Match Files");
    private final JButton openButton = new JButton("Open");
    private final JButton showButton = new JButton("Show in Finder");
    private final JButton removeButton = new JButton("Delete Selected");
    private final JButton selectAllButton = new JButton("Select All");
    private final JButton fullPreviewButton = new JButton("Full Preview");
    private final JButton printButton = new JButton("Print");
    private final JButton downloadButton = new JButton("Download");
    private final RmaDocumentPreviewPanel previewPanel = new RmaDocumentPreviewPanel(storage);
    private final List<RmaAttachment> displayed = new ArrayList<>();

    public RmaDocumentsPanel(String rmaNumber) {
        super(new BorderLayout(8, 8));
        this.rmaNumber = rmaNumber;
        model = new DefaultTableModel(new String[]{"Filename", "Type", "Size (KB)", "Date Added"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { updateActions(); previewPanel.preview(selectedAttachment()); }
        });
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshDocuments());
        addButton.addActionListener(e -> addDocument());
        autoImportButton.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            RmaAutomaticImportDialog dialog = new RmaAutomaticImportDialog(owner, rmaNumber);
            dialog.setVisible(true);
            refreshDocuments();
        });
        openButton.addActionListener(e -> performSelected("open"));
        showButton.addActionListener(e -> performSelected("show"));
        removeButton.addActionListener(e -> removeDocuments());
        selectAllButton.addActionListener(e -> table.selectAll());
        fullPreviewButton.addActionListener(e -> openFullPreview());
        printButton.addActionListener(e -> printDocument());
        downloadButton.addActionListener(e -> downloadDocument());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) openFullPreview();
            }
        });
        JPanel header = new JPanel(new BorderLayout(6, 6));

        JLabel title = new JLabel("Documents associated with this RMA");

        header.add(title, BorderLayout.NORTH);

        JPanel toolbar = createResponsiveToolbar();

        JScrollPane toolbarScroll = new JScrollPane(
                toolbar,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        toolbarScroll.setBorder(null);
        toolbarScroll.setPreferredSize(new Dimension(0, 48));

        header.add(toolbarScroll, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(header, BorderLayout.NORTH);
        top.add(createDropZone(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), previewPanel);
        split.setResizeWeight(0.38);
        split.setDividerLocation(360);
        add(split, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "openAttachment");
        table.getActionMap().put("openAttachment", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { performSelected("open"); }
        });
        refreshDocuments();
    }

    /** Step 10A: file-drop area for the currently open RMA. */
    private JComponent createDropZone() {
        JLabel dropZone = new JLabel("Drop files here to detect RMA numbers or assign manually", SwingConstants.CENTER);
        dropZone.setOpaque(true);
        dropZone.setBackground(UIManager.getColor("Panel.background"));
        dropZone.setBorder(BorderFactory.createDashedBorder(new Color(130, 140, 155), 5, 5));
        dropZone.setPreferredSize(new Dimension(100, 64));
        dropZone.setToolTipText("Drop any files to review and assign them to existing RMAs");
        dropZone.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport support) {
                return rmaNumber != null && !rmaNumber.isBlank()
                        && support.isDrop()
                        && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
            @Override public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                final List<java.io.File> files;
                try {
                    Object data = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!(data instanceof List<?> raw)) return false;
                    files = new ArrayList<>();
                    for (Object item : raw) {
                        if (!(item instanceof java.io.File file) || !file.isFile()) return false;
                        files.add(file);
                    }
                    if (files.isEmpty()) return false;
                } catch (UnsupportedFlavorException | IOException ex) {
                    showError("Could not read dropped files", ex);
                    return false;
                }
                // Use the same review-first workflow as Auto Import PDFs. Never attach
                // a dropped PDF to the currently selected RMA without verifying its number.
                Window owner = SwingUtilities.getWindowAncestor(RmaDocumentsPanel.this);
                RmaAutomaticImportDialog dialog = new RmaAutomaticImportDialog(owner, rmaNumber);
                dialog.analyzeFiles(files.stream().map(java.io.File::toPath).toList());
                dialog.setVisible(true);
                refreshDocuments();
                return true;
            }
        });
        return dropZone;
    }

    public void refreshDocuments() {
        displayed.clear();
        model.setRowCount(0);
        previewPanel.preview(null);
        if (rmaNumber == null || rmaNumber.isBlank()) {
            message.setText("Save the new RMA before attaching documents.");
            updateActions();
            return;
        }
        try {
            displayed.addAll(storage.list(rmaNumber));
            for (RmaAttachment a : displayed) {
                model.addRow(new Object[]{a.getOriginalFileName(),
                        a.getContentType() == null ? "Unknown" : a.getContentType(),
                        String.format("%.1f", a.getFileSize() / 1024.0),
                        a.getDateAdded() == null ? "" : a.getDateAdded().format(DATE_FORMAT)});
            }
            message.setText(displayed.size() + " document(s) attached.");
        } catch (SQLException ex) {
            message.setText("Unable to load documents.");
            showError("Could not load attachments", ex);
        }
        updateActions();
    }

    private void addDocument() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Attach a document to RMA " + rmaNumber);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        int added = 0;
        List<String> failures = new ArrayList<>();
        for (java.io.File file : chooser.getSelectedFiles()) {
            try {
                storage.add(rmaNumber, file.toPath());
                added++;
            } catch (IOException | SQLException | RuntimeException ex) {
                failures.add(file.getName() + ": " + ex.getMessage());
            }
        }
        refreshDocuments();
        if (!failures.isEmpty()) {
            JOptionPane.showMessageDialog(this, added + " file(s) added.\n\nFailed:\n" +
                    String.join("\n", failures), "Attachment results", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openFullPreview() {
        RmaAttachment attachment = selectedAttachment();
        if (attachment == null) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Document Preview — " + attachment.getOriginalFileName(), Dialog.ModalityType.MODELESS);
        RmaDocumentPreviewPanel full = new RmaDocumentPreviewPanel(storage);
        dialog.setContentPane(full);
        dialog.setSize(1000, 750);
        dialog.setLocationRelativeTo(owner);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        full.preview(attachment);
    }

    private RmaAttachment selectedAttachment() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow >= 0 && modelRow < displayed.size() ? displayed.get(modelRow) : null;
    }

    private void performSelected(String action) {
        RmaAttachment a = selectedAttachment();
        if (a == null) return;
        try {
            if ("open".equals(action)) storage.open(a.getId());
            else storage.showInFinder(a.getId());
        } catch (IOException | SQLException | RuntimeException ex) {
            showError("Could not " + ("open".equals(action) ? "open document" : "show document in Finder"), ex);
        }
    }

    /** Save a separate copy of the selected attachment; never move or modify the managed file. */
    private void downloadDocument() {
        RmaAttachment attachment = selectedAttachment();
        if (attachment == null) return;
        try {
            Path source = storage.getFile(attachment.getId());
            Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
            JFileChooser chooser = new JFileChooser(Files.isDirectory(downloads)
                    ? downloads.toFile() : Path.of(System.getProperty("user.home")).toFile());
            chooser.setDialogTitle("Download attachment");
            String original = attachment.getOriginalFileName();
            // Do not let an untrusted attachment name turn into a path outside the chosen folder.
            String safeName = Path.of(original == null || original.isBlank() ? "attachment" : original)
                    .getFileName().toString();
            chooser.setSelectedFile(chooser.getCurrentDirectory().toPath().resolve(safeName).toFile());
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path destination = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            if (Files.isDirectory(destination)) {
                JOptionPane.showMessageDialog(this, "Choose a filename, not a folder.",
                        "Download", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (Files.exists(destination)) {
                int response = JOptionPane.showConfirmDialog(this,
                        "This file already exists:\n" + destination.getFileName()
                                + "\n\nReplace it?", "Confirm replacement",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION) return;
            }
            if (Files.exists(destination) && Files.isSameFile(source, destination)) {
                JOptionPane.showMessageDialog(this, "The selected location is the stored attachment itself.",
                        "Download", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            message.setText("Downloaded: " + destination);
            JOptionPane.showMessageDialog(this, "Saved to:\n" + destination,
                    "Download complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SQLException | RuntimeException ex) {
            showError("Could not download document", ex);
        }
    }

    /** PDF files print via PDFBox; other formats use the OS's registered print handler. */
    private void printDocument() {
        RmaAttachment attachment = selectedAttachment();
        if (attachment == null) return;
        try {
            Path file = storage.getFile(attachment.getId());
            if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file.toFile())) {
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setJobName(attachment.getOriginalFileName());
                    job.setPageable(new PDFPageable(document));
                    if (job.printDialog()) {
                        job.print();
                        message.setText("Sent to printer: " + attachment.getOriginalFileName());
                    }
                }
            } else {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
                    JOptionPane.showMessageDialog(this,
                            "Direct printing is unavailable for this file type. Use Open to print from its application.",
                            "Printing unavailable", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Print this document using its default macOS application?\n"
                                + attachment.getOriginalFileName(),
                        "Print document", JOptionPane.OK_CANCEL_OPTION);
                if (confirm == JOptionPane.OK_OPTION) {
                    Desktop.getDesktop().print(file.toFile());
                    message.setText("Print request sent: " + attachment.getOriginalFileName());
                }
            }
        } catch (IOException | SQLException | PrinterException | RuntimeException ex) {
            showError("Could not print document", ex);
        }
    }

    private void removeDocuments() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) return;
        List<RmaAttachment> attachments = new ArrayList<>();
        for (int viewRow : rows) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < displayed.size()) attachments.add(displayed.get(modelRow));
        }
        if (attachments.isEmpty()) return;
        StringBuilder prompt = new StringBuilder("Permanently delete " + attachments.size()
                + " document(s) from RMA " + rmaNumber + "?\n\n");
        for (int i = 0; i < Math.min(attachments.size(), 8); i++) {
            prompt.append("• ").append(attachments.get(i).getOriginalFileName()).append("\n");
        }
        if (attachments.size() > 8) prompt.append("...and ").append(attachments.size() - 8).append(" more\n");
        prompt.append("\nThe stored copies will be permanently deleted.");
        int response = JOptionPane.showConfirmDialog(this, prompt.toString(),
                "Confirm bulk document deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (response != JOptionPane.YES_OPTION) return;
        int deleted = 0;
        List<String> failures = new ArrayList<>();
        for (RmaAttachment attachment : attachments) {
            try {
                if (storage.remove(attachment.getId())) deleted++;
                else failures.add(attachment.getOriginalFileName() + ": no longer exists");
            } catch (IOException | SQLException | RuntimeException ex) {
                failures.add(attachment.getOriginalFileName() + ": " + ex.getMessage());
            }
        }
        refreshDocuments();
        message.setText(deleted + " document(s) deleted" + (failures.isEmpty() ? "." : "; " + failures.size() + " failed."));
        if (!failures.isEmpty()) {
            JOptionPane.showMessageDialog(this, deleted + " deleted; " + failures.size()
                    + " failed:\n\n" + String.join("\n", failures),
                    "Bulk deletion results", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateActions() {
        addButton.setEnabled(rmaNumber != null && !rmaNumber.isBlank());
        autoImportButton.setEnabled(true);
        int selectionCount = table.getSelectedRowCount();
        boolean selected = selectionCount == 1;
        openButton.setEnabled(selected);
        fullPreviewButton.setEnabled(selected);
        printButton.setEnabled(selected);
        downloadButton.setEnabled(selected);
        showButton.setEnabled(selected);
        removeButton.setEnabled(selectionCount > 0);
        removeButton.setText(selectionCount > 0 ? "Delete Selected (" + selectionCount + ")" : "Delete Selected");
        selectAllButton.setEnabled(table.getRowCount() > 0);
    }

    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(this, title + ":\n" + ex.getMessage(),
                "Documents", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel createResponsiveToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(
                FlowLayout.LEFT, 6, 6
        ));

        toolbar.add(addButton);
        toolbar.add(autoImportButton);
        toolbar.add(openButton);
        toolbar.add(fullPreviewButton);
        toolbar.add(printButton);
        toolbar.add(downloadButton);
        toolbar.add(showButton);
        toolbar.add(selectAllButton);
        toolbar.add(removeButton);

        return toolbar;
    }
}
