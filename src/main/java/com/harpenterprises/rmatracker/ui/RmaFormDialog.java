package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.Status;
import com.harpenterprises.rmatracker.storage.RmaRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class RmaFormDialog extends JDialog {

    private final JTextField rmaNumberField;
    private final JTextField dateSentField;
    private final JTextField dateReceivedField;
    private final JComboBox<Status> statusComboBox;
    private final JTextField outgoingTrackingField;
    private final JTextField returnTrackingField;
    private final JTextArea notesArea;

    private final JTable repairItemsTable;
    private final DefaultTableModel repairItemsTableModel;

    private final JButton addItemButton;
    private final JButton removeItemButton;
    private final JButton saveButton;
    private final JButton cancelButton;

    private final RmaRepository repository;
    private final RmaRecord existingRecord;

    private boolean saved;

    /**
     * Constructor used when creating a new RMA.
     */
    public RmaFormDialog(JFrame parent) {
        this(parent, null);
    }

    /**
     * Constructor used when creating or editing an RMA.
     */
    public RmaFormDialog(
            JFrame parent,
            RmaRecord existingRecord
    ) {
        super(
                parent,
                existingRecord == null ? "New RMA" : "Edit RMA",
                true
        );

        this.existingRecord = existingRecord;
        this.repository = new RmaRepository();
        this.saved = false;

        rmaNumberField = new JTextField(20);
        dateSentField = new JTextField(20);
        dateReceivedField = new JTextField(20);

        statusComboBox = new JComboBox<>(Status.values());

        outgoingTrackingField = new JTextField(20);
        returnTrackingField = new JTextField(20);

        notesArea = new JTextArea(5, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        repairItemsTableModel = new DefaultTableModel(
                new Object[]{
                        "County",
                        "Machine Type",
                        "Serial Number",
                        "Version",
                        "Problem Description",
                        "Repair Description"
                },
                0
        );

        repairItemsTable = new JTable(repairItemsTableModel);
        repairItemsTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        repairItemsTable.setFillsViewportHeight(true);
        repairItemsTable.setRowHeight(24);
        repairItemsTable
                .getTableHeader()
                .setReorderingAllowed(false);

        addItemButton = new JButton("Add Item");
        removeItemButton = new JButton("Remove Item");
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");

        setLayout(new BorderLayout(10, 10));

        add(createFormPanel(), BorderLayout.NORTH);
        add(createRepairItemsPanel(), BorderLayout.CENTER);
        add(createBottomButtonPanel(), BorderLayout.SOUTH);

        addItemButton.addActionListener(
                event -> addRepairItem()
        );

        removeItemButton.addActionListener(
                event -> removeRepairItem()
        );

        saveButton.addActionListener(
                event -> saveRma()
        );

        cancelButton.addActionListener(
                event -> dispose()
        );

        getRootPane().setDefaultButton(saveButton);

        if (existingRecord != null) {
            loadExistingRecord();
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(950, 650));
        setSize(1000, 700);
        setLocationRelativeTo(parent);
    }

    /**
     * MainWindow uses this to know whether it should refresh its table.
     */
    public boolean isSaved() {
        return saved;
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());

        formPanel.setBorder(
                BorderFactory.createTitledBorder("RMA Information")
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.insets = new Insets(5, 8, 5, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        addFormRow(
                formPanel,
                constraints,
                row++,
                "RMA Number:",
                rmaNumberField
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Date Sent (YYYY-MM-DD):",
                dateSentField
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Date Received (YYYY-MM-DD):",
                dateReceivedField
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Status:",
                statusComboBox
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Outgoing Tracking:",
                outgoingTrackingField
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Return Tracking:",
                returnTrackingField
        );

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.NONE;

        formPanel.add(
                new JLabel("Notes:"),
                constraints
        );

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.BOTH;

        JScrollPane notesScrollPane =
                new JScrollPane(notesArea);

        notesScrollPane.setPreferredSize(
                new Dimension(500, 100)
        );

        formPanel.add(
                notesScrollPane,
                constraints
        );

        return formPanel;
    }

    private void addFormRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String labelText,
            JComponent component
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;

        panel.add(
                new JLabel(labelText),
                constraints
        );

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        panel.add(component, constraints);
    }

    private JPanel createRepairItemsPanel() {
        JPanel repairItemsPanel =
                new JPanel(new BorderLayout(5, 5));

        repairItemsPanel.setBorder(
                BorderFactory.createTitledBorder("Repair Items")
        );

        JScrollPane tableScrollPane =
                new JScrollPane(repairItemsTable);

        repairItemsPanel.add(
                tableScrollPane,
                BorderLayout.CENTER
        );

        JPanel itemButtonPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        itemButtonPanel.add(addItemButton);
        itemButtonPanel.add(removeItemButton);

        repairItemsPanel.add(
                itemButtonPanel,
                BorderLayout.SOUTH
        );

        return repairItemsPanel;
    }

    private JPanel createBottomButtonPanel() {
        JPanel buttonPanel =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 10, 10, 10)
        );

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }

    private void addRepairItem() {
        repairItemsTableModel.addRow(
                new Object[]{
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                }
        );

        int newRow =
                repairItemsTableModel.getRowCount() - 1;

        repairItemsTable.setRowSelectionInterval(
                newRow,
                newRow
        );

        repairItemsTable.editCellAt(newRow, 0);
        repairItemsTable.requestFocusInWindow();
    }

    private void removeRepairItem() {
        int selectedRow =
                repairItemsTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a repair item to remove.",
                    "No Item Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        repairItemsTableModel.removeRow(selectedRow);
    }

    private void loadExistingRecord() {
        rmaNumberField.setText(
                existingRecord.getRmaNumber()
        );

        /*
         * The RMA number identifies the existing database record,
         * so it cannot be changed while editing.
         */
        rmaNumberField.setEditable(false);

        dateSentField.setText(
                formatDate(existingRecord.getDateSent())
        );

        dateReceivedField.setText(
                formatDate(existingRecord.getDateReceived())
        );

        statusComboBox.setSelectedItem(
                existingRecord.getStatus()
        );

        outgoingTrackingField.setText(
                emptyIfNull(
                        existingRecord.getOutgoingTrackingNumber()
                )
        );

        returnTrackingField.setText(
                emptyIfNull(
                        existingRecord.getReturnTrackingNumber()
                )
        );

        notesArea.setText(
                emptyIfNull(existingRecord.getNotes())
        );

        for (RepairItem item :
                existingRecord.getRepairItems()) {

            repairItemsTableModel.addRow(
                    new Object[]{
                            emptyIfNull(item.getCounty()),
                            emptyIfNull(item.getMachineType()),
                            emptyIfNull(item.getSerialNumber()),
                            emptyIfNull(item.getVersion()),
                            emptyIfNull(
                                    item.getProblemDescription()
                            ),
                            emptyIfNull(
                                    item.getRepairDescription()
                            )
                    }
            );
        }
    }

    private void saveRma() {
        stopTableEditing();

        String rmaNumber =
                rmaNumberField.getText().trim();

        if (rmaNumber.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The RMA number is required.",
                    "Missing RMA Number",
                    JOptionPane.WARNING_MESSAGE
            );

            rmaNumberField.requestFocusInWindow();
            return;
        }

        try {
            LocalDate dateSent = parseOptionalDate(
                    dateSentField.getText()
            );

            LocalDate dateReceived = parseOptionalDate(
                    dateReceivedField.getText()
            );

            Status status =
                    (Status) statusComboBox.getSelectedItem();

            if (status == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Select an RMA status.",
                        "Missing Status",
                        JOptionPane.WARNING_MESSAGE
                );

                statusComboBox.requestFocusInWindow();
                return;
            }

            List<RepairItem> repairItems =
                    readRepairItems();

            RmaRecord record = new RmaRecord(
                    rmaNumber,
                    dateSent,
                    dateReceived,
                    status,
                    outgoingTrackingField
                            .getText()
                            .trim(),
                    returnTrackingField
                            .getText()
                            .trim(),
                    notesArea
                            .getText()
                            .trim(),
                    repairItems
            );

            if (existingRecord == null) {
                boolean savedNewRecord =
                        saveNewRecord(record);

                if (!savedNewRecord) {
                    return;
                }
            } else {
                repository.update(record);
            }

            saved = true;

            JOptionPane.showMessageDialog(
                    this,
                    existingRecord == null
                            ? "The RMA was saved successfully."
                            : "The RMA was updated successfully.",
                    "RMA Saved",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dispose();

        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Dates must use the format YYYY-MM-DD.\n"
                            + "Example: 2026-07-13",
                    "Invalid Date",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The RMA could not be saved.\n\n"
                            + exception.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );

            exception.printStackTrace();
        }
    }

    private boolean saveNewRecord(
            RmaRecord record
    ) throws SQLException {

        if (repository.existsByRmaNumber(
                record.getRmaNumber()
        )) {
            JOptionPane.showMessageDialog(
                    this,
                    "An RMA with number "
                            + record.getRmaNumber()
                            + " already exists.",
                    "Duplicate RMA Number",
                    JOptionPane.WARNING_MESSAGE
            );

            rmaNumberField.requestFocusInWindow();
            return false;
        }

        repository.save(record);
        return true;
    }

    private List<RepairItem> readRepairItems() {
        List<RepairItem> repairItems =
                new ArrayList<>();

        for (int row = 0;
             row < repairItemsTableModel.getRowCount();
             row++) {

            String county =
                    getTableValue(row, 0);

            String machineType =
                    getTableValue(row, 1);

            String serialNumber =
                    getTableValue(row, 2);

            String version =
                    getTableValue(row, 3);

            String problemDescription =
                    getTableValue(row, 4);

            String repairDescription =
                    getTableValue(row, 5);

            /*
             * Ignore a completely empty repair-item row.
             */
            if (county.isBlank()
                    && machineType.isBlank()
                    && serialNumber.isBlank()
                    && version.isBlank()
                    && problemDescription.isBlank()
                    && repairDescription.isBlank()) {

                continue;
            }

            RepairItem repairItem = new RepairItem(
                    county,
                    machineType,
                    serialNumber,
                    version,
                    problemDescription,
                    repairDescription
            );

            repairItems.add(repairItem);
        }

        return repairItems;
    }

    private String getTableValue(
            int row,
            int column
    ) {
        Object value =
                repairItemsTableModel.getValueAt(
                        row,
                        column
                );

        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }

    private LocalDate parseOptionalDate(
            String dateText
    ) {
        String trimmedDate =
                dateText.trim();

        if (trimmedDate.isEmpty()) {
            return null;
        }

        return LocalDate.parse(trimmedDate);
    }

    private void stopTableEditing() {
        if (repairItemsTable.isEditing()) {
            repairItemsTable
                    .getCellEditor()
                    .stopCellEditing();
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }

        return date.toString();
    }

    private String emptyIfNull(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }
}