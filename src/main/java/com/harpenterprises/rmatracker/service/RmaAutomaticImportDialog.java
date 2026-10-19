package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.service.RmaAutomaticAttachmentService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Step 8E: review-first bulk PDF import. Does not import conflicts or unknown RMAs. */
public final class RmaAutomaticImportDialog extends JDialog {
    private final RmaAutomaticAttachmentService service = new RmaAutomaticAttachmentService();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"File", "Detected RMA", "Result", "Details"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final List<RmaAutomaticAttachmentService.Preview> previews = new ArrayList<>();
    private final JButton importButton = new JButton("Import Verified Files");
    private final JLabel summary = new JLabel("Choose PDF files to analyze.");

    public RmaAutomaticImportDialog(Window owner) {
        super(owner, "Automatic RMA Document Import", ModalityType.APPLICATION_MODAL);
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(25);
        JButton select = new JButton("Select PDFs...");
        select.addActionListener(e -> selectFiles());
        importButton.setEnabled(false);
        importButton.addActionListener(e -> importVerified());
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(select); actions.add(importButton); actions.add(close);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(summary, BorderLayout.NORTH);
        add(actions, BorderLayout.SOUTH);
        setSize(850, 430);
        setLocationRelativeTo(owner);
    }

    private void selectFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF documents", "pdf"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        previews.clear(); model.setRowCount(0);
        int ready = 0;
        for (java.io.File f : chooser.getSelectedFiles()) {
            Path file = f.toPath();
            try {
                var preview = service.preview(file);
                previews.add(preview);
                if (preview.canImport()) ready++;
                model.addRow(new Object[]{f.getName(), preview.rmaNumber() == null ? "—" : preview.rmaNumber(),
                        preview.status().name(), preview.detectionStatus().name()});
            } catch (Exception ex) {
                model.addRow(new Object[]{f.getName(), "—", "ERROR", ex.getMessage()});
            }
        }
        summary.setText(ready + " verified / " + chooser.getSelectedFiles().length + " selected. Unverified files will NOT be imported.");
        importButton.setEnabled(ready > 0);
    }

    private void importVerified() {
        long count = previews.stream().filter(RmaAutomaticAttachmentService.Preview::canImport).count();
        int answer = JOptionPane.showConfirmDialog(this,
                "Import " + count + " verified document(s) into their matching RMAs?\nConflicts and unmatched files will be skipped.",
                "Confirm automatic import", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        int success = 0;
        List<String> failures = new ArrayList<>();
        for (var preview : previews) {
            if (!preview.canImport()) continue;
            try { service.importConfirmed(preview); success++; }
            catch (Exception ex) { failures.add(preview.file().getFileName() + ": " + ex.getMessage()); }
        }
        importButton.setEnabled(false);
        summary.setText(success + " document(s) imported. Close and refresh RMA Documents to see them.");
        JOptionPane.showMessageDialog(this, "Imported: " + success + "\nFailed: " + failures.size() +
                (failures.isEmpty() ? "" : "\n\n" + String.join("\n", failures)), "Import results",
                failures.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }
}
