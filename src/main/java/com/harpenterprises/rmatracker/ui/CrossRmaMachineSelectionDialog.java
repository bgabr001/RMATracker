package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.storage.RmaRepository;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.nio.file.Path;
import java.io.IOException;
import java.util.stream.Collectors;
import com.harpenterprises.rmatracker.service.SelectedMachineReportService;
import com.harpenterprises.rmatracker.service.SelectedMachineReportService.Machine;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Phase 4.4: transient cross-RMA machine selection, without database mutations. */
public final class CrossRmaMachineSelectionDialog extends JDialog {
    private static final class Entry {
        final String rma;
        final RepairItem item;
        boolean selected;
        Entry(String rma, RepairItem item) { this.rma = rma; this.item = item; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final JTextField search = new JTextField(24);
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final JLabel count = new JLabel();
    private final AbstractTableModel model = new AbstractTableModel() {
        final String[] columns = {"Select", "RMA #", "County", "Serial #", "Machine Type", "Version", "Received"};
        public int getRowCount() { return entries.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int c) { return columns[c]; }
        public Class<?> getColumnClass(int c) { return c == 0 || c == 6 ? Boolean.class : String.class; }
        public boolean isCellEditable(int r, int c) { return c == 0; }
        public Object getValueAt(int r, int c) {
            Entry e = entries.get(r);
            switch (c) {
                case 0: return e.selected;
                case 1: return e.rma;
                case 2: return safe(e.item.getCounty());
                case 3: return safe(e.item.getSerialNumber());
                case 4: return safe(e.item.getMachineType());
                case 5: return safe(e.item.getVersion());
                default: return e.item.isReceived();
            }
        }
        public void setValueAt(Object value, int r, int c) {
            if (c == 0) { entries.get(r).selected = Boolean.TRUE.equals(value); fireTableDataChanged(); updateCount(); }
        }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<AbstractTableModel> sorter;

    public CrossRmaMachineSelectionDialog(Window owner) {
        super(owner, "Cross-RMA Machine Selection", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1080, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8,8));
        try {
            for (RmaRecord r : new RmaRepository().findAll()) {
                if (r.getRepairItems() != null) for (RepairItem item : r.getRepairItems()) {
                    entries.add(new Entry(r.getRmaNumber(), item));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(owner, "Could not load machines: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        // Create sorter after the model contains all machines.
        sorter = new TableRowSorter<>(model);
        typeFilter.addItem("All machine types");
        entries.stream().map(e -> safe(e.item.getMachineType())).filter(s -> !s.isBlank())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).forEach(typeFilter::addItem);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Search:")); filters.add(search);
        filters.add(new JLabel("Type:")); filters.add(typeFilter);
        JButton clearFilters = new JButton("Clear Filters");
        clearFilters.addActionListener(e -> { search.setText(""); typeFilter.setSelectedIndex(0); });
        filters.add(clearFilters);
        add(filters, BorderLayout.NORTH);
        table.setRowSorter(sorter);
        table.setAutoCreateRowSorter(false);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setMaxWidth(65);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectVisible = new JButton("Select Visible");
        selectVisible.addActionListener(e -> setVisibleSelection(true));
        JButton deselectVisible = new JButton("Deselect Visible");
        deselectVisible.addActionListener(e -> setVisibleSelection(false));
        JButton clearAll = new JButton("Clear All Selections");
        clearAll.addActionListener(e -> { entries.forEach(x -> x.selected = false); model.fireTableDataChanged(); updateCount(); });
        bottom.add(selectVisible); bottom.add(deselectVisible); bottom.add(clearAll);
        JButton preview = new JButton("Print Preview / Print");
        preview.addActionListener(e -> previewReport());
        JButton excel = new JButton("Export Excel");
        excel.addActionListener(e -> exportReport(true));
        JButton pdf = new JButton("Export PDF");
        pdf.addActionListener(e -> exportReport(false));
        bottom.add(preview); bottom.add(excel); bottom.add(pdf); bottom.add(count);
        add(bottom, BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        typeFilter.addActionListener(e -> applyFilter());
        applyFilter();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private void applyFilter() {
        String term = search.getText().trim().toLowerCase(Locale.ROOT);
        String type = (String) typeFilter.getSelectedItem();
        sorter.setRowFilter(new RowFilter<AbstractTableModel, Integer>() {
            @Override public boolean include(Entry<? extends AbstractTableModel, ? extends Integer> row) {
                int index = row.getIdentifier();
                CrossRmaMachineSelectionDialog.Entry e = entries.get(index);
                boolean matchesType = type == null || "All machine types".equals(type)
                        || safe(e.item.getMachineType()).equalsIgnoreCase(type);
                String all = (safe(e.rma) + " " + safe(e.item.getCounty()) + " "
                        + safe(e.item.getSerialNumber()) + " " + safe(e.item.getMachineType()) + " "
                        + safe(e.item.getVersion())).toLowerCase(Locale.ROOT);
                return matchesType && all.contains(term);
            }
        });
        updateCount();
    }
    private void setVisibleSelection(boolean selected) {
        for (int view = 0; view < table.getRowCount(); view++) {
            entries.get(table.convertRowIndexToModel(view)).selected = selected;
        }
        model.fireTableDataChanged();
        updateCount();
    }
    private void updateCount() {
        long n = entries.stream().filter(e -> e.selected).count();
        count.setText("Selected: " + n + " of " + entries.size() + " machines (" + table.getRowCount() + " visible)");
    }
    private List<Machine> selectedMachines() {
        return entries.stream().filter(e -> e.selected)
            .map(e -> new Machine(e.rma, e.item.getCounty(), e.item.getSerialNumber(),
                e.item.getMachineType(), e.item.getVersion(), e.item.isReceived()))
            .collect(Collectors.toList());
    }
    private void previewReport() {
        List<Machine> chosen = selectedMachines();
        if (chosen.isEmpty()) { JOptionPane.showMessageDialog(this,"Select at least one machine first."); return; }
        JEditorPane pane = new JEditorPane("text/html", SelectedMachineReportService.html(chosen));
        pane.setEditable(false);
        JDialog dialog = new JDialog(this,"Selected Machines — Print Preview",ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(new JScrollPane(pane),BorderLayout.CENTER);
        JButton print = new JButton("Print...");
        print.addActionListener(e -> {
            try { pane.print(); } catch (java.awt.print.PrinterException ex) {
                JOptionPane.showMessageDialog(dialog,ex.getMessage(),"Print Error",JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton close = new JButton("Close"); close.addActionListener(e -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));actions.add(print);actions.add(close);
        dialog.add(actions,BorderLayout.SOUTH);dialog.setSize(950,650);dialog.setLocationRelativeTo(this);dialog.setVisible(true);
    }
    private void exportReport(boolean excel) {
        List<Machine> chosen = selectedMachines();
        if (chosen.isEmpty()) { JOptionPane.showMessageDialog(this,"Select at least one machine first."); return; }
        String ext = excel ? ".xlsx" : ".pdf";
        JFileChooser chooser = new JFileChooser(Path.of(System.getProperty("user.home"),"Downloads").toFile());
        chooser.setSelectedFile(Path.of(System.getProperty("user.home"),"Downloads","selected-machines"+ext).toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(ext))
            file = Path.of(file.toString()+ext);
        if (java.nio.file.Files.exists(file) && JOptionPane.showConfirmDialog(this,
                "Replace existing file?\n"+file,"Confirm Overwrite",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            if(excel) SelectedMachineReportService.excel(chosen,file);
            else SelectedMachineReportService.pdf(chosen,file);
            JOptionPane.showMessageDialog(this,"Exported "+chosen.size()+" machines to:\n"+file);
        } catch(IOException ex) { JOptionPane.showMessageDialog(this,ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE); }
    }

}
