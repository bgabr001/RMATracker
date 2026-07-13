package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.Status;
import com.harpenterprises.rmatracker.model.ShippingInfo;
import com.harpenterprises.rmatracker.model.ShippingDirection;
import com.harpenterprises.rmatracker.storage.RmaRepository;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class RmaFormDialog extends JDialog {

    /*
     * RMA fields
     */
    private final JTextField rmaNumberField;
    private final JTextField dateSentField;
    private final JTextField dateReceivedField;
    private final JComboBox<Status> statusComboBox;
    private final JTextField outgoingTrackingField;
    private final JTextField returnTrackingField;
    private final JTextArea notesArea;

    /*
     * Repair-item table
     */
    private final JTable repairItemsTable;
    private final DefaultTableModel repairItemsTableModel;
    private final JTable shippingTable;
    private final DefaultTableModel shippingTableModel;

    /*
     * Buttons
     */
    private final JButton addItemButton;
    private final JButton removeItemButton;
    private final JButton addShippingButton;
    private final JButton removeShippingButton;
    private final JButton saveButton;
    private final JButton cancelButton;

    /*
     * Data
     */
    private final RmaRepository repository;
    private final RmaRecord existingRecord;

    /*
     * Form state
     */
    private boolean saved;
    private boolean formChanged;
    private boolean loadingExistingRecord;

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
                existingRecord == null
                        ? "New RMA"
                        : "Edit RMA",
                true
        );

        this.existingRecord = existingRecord;
        this.repository = new RmaRepository();

        saved = false;
        formChanged = false;
        loadingExistingRecord = true;

        /*
         * Create RMA fields.
         */
        rmaNumberField = new JTextField(20);
        dateSentField = new JTextField(20);
        dateReceivedField = new JTextField(20);

        dateSentField.setToolTipText(
                "Enter the date as YYYY-MM-DD"
        );

        dateReceivedField.setToolTipText(
                "Enter the date as YYYY-MM-DD"
        );

        statusComboBox =
                new JComboBox<>(Status.values());

        outgoingTrackingField =
                new JTextField(20);

        returnTrackingField =
                new JTextField(20);

        notesArea = new JTextArea(5, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        shippingTableModel = new DefaultTableModel(
                new Object[]{"Carrier", "Shipping Number", "Direction"}, 0
        ) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? ShippingDirection.class : String.class;
            }
        };

        shippingTable = new JTable(shippingTableModel);
        shippingTable.setRowHeight(24);
        shippingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shippingTable.getTableHeader().setReorderingAllowed(false);
        shippingTable.getColumnModel().getColumn(2).setCellEditor(
                new DefaultCellEditor(new JComboBox<>(ShippingDirection.values()))
        );

        /*
         * Create repair-item table.
         */
        repairItemsTableModel =
                new DefaultTableModel(
                        new Object[]{
                                "County",
                                "Machine Type",
                                "Serial Number",
                                "Version",
                                "Problem Description",
                                "Repair Description",
                                "Received"
                        },
                        0
                ) {
                    @Override
                    public Class<?> getColumnClass(int columnIndex) {
                        return columnIndex == 6 ? Boolean.class : String.class;
                    }
                };

        repairItemsTable =
                new JTable(repairItemsTableModel);

        repairItemsTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        repairItemsTable.setFillsViewportHeight(true);
        repairItemsTable.setRowHeight(24);

        repairItemsTable
                .getTableHeader()
                .setReorderingAllowed(false);

        JComboBox<String> machineTypeEditor = new JComboBox<>(
                new String[]{
                        "Scanner",
                        "Duo Standalone",
                        "Touch Writer",
                        "Duo",
                        "Print"
                }
        );
        repairItemsTable.getColumnModel()
                .getColumn(1)
                .setCellEditor(new DefaultCellEditor(machineTypeEditor));

        configureRepairItemColumnWidths();

        /*
         * Create buttons.
         */
        addItemButton = new JButton("Add Item");
        removeItemButton = new JButton("Remove Item");
        addShippingButton = new JButton("+");
        addShippingButton.setToolTipText("Add shipping information");
        removeShippingButton = new JButton("Remove Shipping");
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");

        /*
         * Build window.
         */
        setLayout(new BorderLayout(10, 10));

        add(
                createFormPanel(),
                BorderLayout.NORTH
        );

        add(
                createCenterDataPanel(),
                BorderLayout.CENTER
        );

        add(
                createBottomButtonPanel(),
                BorderLayout.SOUTH
        );

        addListeners();
        addKeyboardShortcuts();

        /*
         * Load record after the listeners exist.
         * loadingExistingRecord prevents loading from being
         * treated as a user edit.
         */
        if (existingRecord != null) {
            loadExistingRecord();
        }

        loadingExistingRecord = false;
        formChanged = false;

        getRootPane().setDefaultButton(saveButton);

        /*
         * Do not immediately close when the X is clicked.
         * requestClose() checks for unsaved changes first.
         */
        setDefaultCloseOperation(
                DO_NOTHING_ON_CLOSE
        );

        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(
                            WindowEvent event
                    ) {
                        requestClose();
                    }
                }
        );

        setMinimumSize(
                new Dimension(950, 650)
        );

        setSize(1000, 700);
        setLocationRelativeTo(parent);
    }

    /**
     * MainWindow uses this to know whether it should
     * refresh its table.
     */
    public boolean isSaved() {
        return saved;
    }

    private JPanel createFormPanel() {
        JPanel formPanel =
                new JPanel(new GridBagLayout());

        formPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "RMA Information"
                )
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.insets =
                new Insets(5, 8, 5, 8);

        constraints.anchor =
                GridBagConstraints.WEST;

        constraints.fill =
                GridBagConstraints.HORIZONTAL;

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

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.anchor =
                GridBagConstraints.NORTHWEST;

        constraints.fill =
                GridBagConstraints.NONE;

        formPanel.add(
                new JLabel("Notes:"),
                constraints
        );

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill =
                GridBagConstraints.BOTH;

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
        constraints.fill =
                GridBagConstraints.NONE;

        panel.add(
                new JLabel(labelText),
                constraints
        );

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        panel.add(component, constraints);
    }

    private JPanel createCenterDataPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(createShippingPanel(), BorderLayout.NORTH);
        panel.add(createRepairItemsPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createShippingPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Shipping Information"));
        JScrollPane scrollPane = new JScrollPane(shippingTable);
        scrollPane.setPreferredSize(new Dimension(850, 115));
        panel.add(scrollPane, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addShippingButton);
        buttons.add(new JLabel("Add another shipment"));
        buttons.add(removeShippingButton);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRepairItemsPanel() {
        JPanel repairItemsPanel =
                new JPanel(new BorderLayout(5, 5));

        repairItemsPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Repair Items"
                )
        );

        JScrollPane tableScrollPane =
                new JScrollPane(repairItemsTable);

        repairItemsPanel.add(
                tableScrollPane,
                BorderLayout.CENTER
        );

        JPanel itemButtonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

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
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttonPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        10,
                        10,
                        10
                )
        );

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }

    private void configureRepairItemColumnWidths() {
        int[] widths = {
                130,
                140,
                150,
                100,
                260,
                260,
                90
        };

        for (int column = 0;
             column < widths.length;
             column++) {

            repairItemsTable
                    .getColumnModel()
                    .getColumn(column)
                    .setPreferredWidth(widths[column]);
        }

        repairItemsTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF
        );
    }

    private void addListeners() {
        addShippingButton.addActionListener(event -> addShippingRow());
        removeShippingButton.addActionListener(event -> removeShippingRow());

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
                event -> requestClose()
        );

        /*
         * Watch text fields and the notes area for changes.
         */
        DocumentListener changeListener =
                new DocumentListener() {

                    @Override
                    public void insertUpdate(
                            DocumentEvent event
                    ) {
                        markFormChanged();
                    }

                    @Override
                    public void removeUpdate(
                            DocumentEvent event
                    ) {
                        markFormChanged();
                    }

                    @Override
                    public void changedUpdate(
                            DocumentEvent event
                    ) {
                        markFormChanged();
                    }
                };

        rmaNumberField
                .getDocument()
                .addDocumentListener(changeListener);

        dateSentField
                .getDocument()
                .addDocumentListener(changeListener);

        dateReceivedField
                .getDocument()
                .addDocumentListener(changeListener);

        outgoingTrackingField
                .getDocument()
                .addDocumentListener(changeListener);

        returnTrackingField
                .getDocument()
                .addDocumentListener(changeListener);

        notesArea
                .getDocument()
                .addDocumentListener(changeListener);

        statusComboBox.addActionListener(
                event -> markFormChanged()
        );

        shippingTableModel.addTableModelListener(event -> markFormChanged());

        repairItemsTableModel.addTableModelListener(
                event -> {
                    if (event.getType()
                            == TableModelEvent.UPDATE
                            || event.getType()
                            == TableModelEvent.INSERT
                            || event.getType()
                            == TableModelEvent.DELETE) {

                        markFormChanged();
                    }
                }
        );
    }

    private void addKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();

        InputMap inputMap =
                rootPane.getInputMap(
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

        ActionMap actionMap =
                rootPane.getActionMap();

        /*
         * Escape closes the form through the confirmation method.
         */
        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_ESCAPE,
                        0
                ),
                "closeForm"
        );

        actionMap.put(
                "closeForm",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent event
                    ) {
                        requestClose();
                    }
                }
        );

        /*
         * Command+S on macOS or Ctrl+S on Windows/Linux.
         */
        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_S,
                        Toolkit
                                .getDefaultToolkit()
                                .getMenuShortcutKeyMaskEx()
                ),
                "saveForm"
        );

        actionMap.put(
                "saveForm",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent event
                    ) {
                        saveRma();
                    }
                }
        );
    }

    private void markFormChanged() {
        if (!loadingExistingRecord && !saved) {
            formChanged = true;
        }
    }

    private void addShippingRow() {
        stopTableEditing();
        shippingTableModel.addRow(new Object[]{"", "", ShippingDirection.SENT_TO_REPAIR});
        int row = shippingTableModel.getRowCount() - 1;
        shippingTable.setRowSelectionInterval(row, row);
        shippingTable.editCellAt(row, 0);
        shippingTable.requestFocusInWindow();
        formChanged = true;
    }

    private void removeShippingRow() {
        stopTableEditing();
        int row = shippingTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select shipping information to remove.",
                    "No Shipping Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        shippingTableModel.removeRow(shippingTable.convertRowIndexToModel(row));
        formChanged = true;
    }

    private void addRepairItem() {
        stopTableEditing();

        repairItemsTableModel.addRow(
                new Object[]{
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        Boolean.FALSE
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

        formChanged = true;
    }

    private void removeRepairItem() {
        stopTableEditing();

        int selectedViewRow =
                repairItemsTable.getSelectedRow();

        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a repair item to remove.",
                    "No Item Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int selectedModelRow =
                repairItemsTable.convertRowIndexToModel(
                        selectedViewRow
                );

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to remove "
                                + "the selected repair item?",
                        "Remove Repair Item",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        repairItemsTableModel.removeRow(
                selectedModelRow
        );

        formChanged = true;
    }

    private void loadExistingRecord() {
        rmaNumberField.setText(
                existingRecord.getRmaNumber()
        );

        /*
         * The RMA number identifies the database
         * record, so it cannot be changed while editing.
         */
        rmaNumberField.setEditable(false);

        dateSentField.setText(
                formatDate(
                        existingRecord.getDateSent()
                )
        );

        dateReceivedField.setText(
                formatDate(
                        existingRecord.getDateReceived()
                )
        );

        statusComboBox.setSelectedItem(
                existingRecord.getStatus()
        );

        outgoingTrackingField.setText(
                emptyIfNull(
                        existingRecord
                                .getOutgoingTrackingNumber()
                )
        );

        returnTrackingField.setText(
                emptyIfNull(
                        existingRecord
                                .getReturnTrackingNumber()
                )
        );

        notesArea.setText(
                emptyIfNull(
                        existingRecord.getNotes()
                )
        );

        List<ShippingInfo> existingShipping = existingRecord.getShippingInformation();
        if (existingShipping != null) {
            for (ShippingInfo shipping : existingShipping) {
                shippingTableModel.addRow(new Object[]{
                        emptyIfNull(shipping.getCarrier()),
                        emptyIfNull(shipping.getTrackingNumber()),
                        shipping.getDirection()
                });
            }
        }

        for (RepairItem item :
                existingRecord.getRepairItems()) {

            repairItemsTableModel.addRow(
                    new Object[]{
                            emptyIfNull(
                                    item.getCounty()
                            ),
                            emptyIfNull(
                                    item.getMachineType()
                            ),
                            emptyIfNull(
                                    item.getSerialNumber()
                            ),
                            emptyIfNull(
                                    item.getVersion()
                            ),
                            emptyIfNull(
                                    item.getProblemDescription()
                            ),
                            emptyIfNull(
                                    item.getRepairDescription()
                            ),
                            item.isReceived()
                    }
            );
        }
    }

    private void saveRma() {
        stopTableEditing();

        String rmaNumber =
                rmaNumberField
                        .getText()
                        .trim();

        /*
         * Validate RMA number.
         */
        if (rmaNumber.isEmpty()) {
            showValidationMessage(
                    "The RMA number is required.",
                    "Missing RMA Number",
                    rmaNumberField
            );

            return;
        }

        /*
         * Prevent extremely long or accidental RMA values.
         */
        if (rmaNumber.length() > 100) {
            showValidationMessage(
                    "The RMA number cannot be longer "
                            + "than 100 characters.",
                    "Invalid RMA Number",
                    rmaNumberField
            );

            return;
        }

        try {
            LocalDate dateSent =
                    parseOptionalDate(
                            dateSentField.getText()
                    );

            LocalDate dateReceived =
                    parseOptionalDate(
                            dateReceivedField.getText()
                    );

            /*
             * Received date should not be before sent date.
             */
            if (dateSent != null
                    && dateReceived != null
                    && dateReceived.isBefore(dateSent)) {

                showValidationMessage(
                        "The Date Received cannot be before "
                                + "the Date Sent.",
                        "Invalid Date Range",
                        dateReceivedField
                );

                return;
            }

            Status status =
                    (Status) statusComboBox
                            .getSelectedItem();

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

            List<ShippingInfo> shippingInformation =
                    readAndValidateShippingInformation();

            if (shippingInformation == null) {
                return;
            }

            List<RepairItem> repairItems =
                    readAndValidateRepairItems();

            if (repairItems == null) {
                return;
            }

            RmaRecord record =
                    new RmaRecord(
                            rmaNumber,
                            dateSent,
                            dateReceived,
                            status,
                            "",
                            "",
                            notesArea
                                    .getText()
                                    .trim(),
                            repairItems,
                            shippingInformation
                    );

            if (existingRecord == null) {
                if (!saveNewRecord(record)) {
                    return;
                }
            } else {
                repository.update(record);
            }

            saved = true;
            formChanged = false;

            JOptionPane.showMessageDialog(
                    this,
                    existingRecord == null
                            ? "The RMA was saved successfully."
                            : "The RMA was updated successfully.",
                    existingRecord == null
                            ? "RMA Saved"
                            : "RMA Updated",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dispose();

        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Dates must use the format YYYY-MM-DD.\n\n"
                            + "Example: 2026-07-13",
                    "Invalid Date",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (SQLException exception) {
            showDatabaseError(
                    "The RMA could not be saved.",
                    exception
            );

        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "An unexpected error occurred while "
                            + "saving the RMA.\n\n"
                            + "Please check the information "
                            + "and try again.",
                    "Unexpected Error",
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
                            + " already exists.\n\n"
                            + "Enter a different RMA number.",
                    "Duplicate RMA Number",
                    JOptionPane.WARNING_MESSAGE
            );

            rmaNumberField.requestFocusInWindow();
            rmaNumberField.selectAll();

            return false;
        }

        repository.save(record);
        return true;
    }

    /**
     * Reads and validates all repair-item rows.
     *
     * Returns null when validation fails.
     */
    private List<ShippingInfo> readAndValidateShippingInformation() {
        List<ShippingInfo> shipments = new ArrayList<>();
        for (int row = 0; row < shippingTableModel.getRowCount(); row++) {
            String carrier = getShippingValue(row, 0);
            String tracking = getShippingValue(row, 1);
            Object directionValue = shippingTableModel.getValueAt(row, 2);
            ShippingDirection direction = directionValue instanceof ShippingDirection
                    ? (ShippingDirection) directionValue
                    : ShippingDirection.SENT_TO_REPAIR;
            if (carrier.isBlank() && tracking.isBlank()) continue;
            if (carrier.isBlank() || tracking.isBlank()) {
                JOptionPane.showMessageDialog(this,
                        "Carrier and Shipping Number are both required for shipping row " + (row + 1) + ".",
                        "Incomplete Shipping Information", JOptionPane.WARNING_MESSAGE);
                shippingTable.setRowSelectionInterval(row, row);
                return null;
            }
            shipments.add(new ShippingInfo(carrier, tracking, direction));
        }
        return shipments;
    }

    private String getShippingValue(int row, int column) {
        Object value = shippingTableModel.getValueAt(row, column);
        return value == null ? "" : value.toString().trim();
    }

    private List<RepairItem>
    readAndValidateRepairItems() {

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

            boolean received =
                    getTableBooleanValue(row, 6);

            boolean completelyEmpty =
                    county.isBlank()
                            && machineType.isBlank()
                            && serialNumber.isBlank()
                            && version.isBlank()
                            && problemDescription.isBlank()
                            && repairDescription.isBlank();

            /*
             * Completely empty rows are ignored.
             */
            if (completelyEmpty) {
                continue;
            }

            /*
             * County and machine type are required for
             * any repair-item row that contains information.
             */
            if (county.isBlank()) {
                showRepairItemValidationError(
                        row,
                        0,
                        "County is required for repair item "
                                + (row + 1) + "."
                );

                return null;
            }

            if (machineType.isBlank()) {
                showRepairItemValidationError(
                        row,
                        1,
                        "Machine Type is required for repair item "
                                + (row + 1) + "."
                );

                return null;
            }

            RepairItem repairItem =
                    new RepairItem(
                            county,
                            machineType,
                            serialNumber,
                            version,
                            problemDescription,
                            repairDescription,
                            received
                    );

            repairItems.add(repairItem);
        }

        return repairItems;
    }

    private void showRepairItemValidationError(
            int modelRow,
            int modelColumn,
            String message
    ) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Incomplete Repair Item",
                JOptionPane.WARNING_MESSAGE
        );

        int viewRow =
                repairItemsTable
                        .convertRowIndexToView(modelRow);

        int viewColumn =
                repairItemsTable
                        .convertColumnIndexToView(modelColumn);

        if (viewRow >= 0 && viewColumn >= 0) {
            repairItemsTable.setRowSelectionInterval(
                    viewRow,
                    viewRow
            );

            repairItemsTable.setColumnSelectionInterval(
                    viewColumn,
                    viewColumn
            );

            repairItemsTable.editCellAt(
                    viewRow,
                    viewColumn
            );

            repairItemsTable.requestFocusInWindow();
        }
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

        return value
                .toString()
                .trim();
    }

    private boolean getTableBooleanValue(
            int row,
            int column
    ) {
        Object value = repairItemsTableModel.getValueAt(row, column);
        return value instanceof Boolean && (Boolean) value;
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

    private void requestClose() {
        stopTableEditing();

        /*
         * No confirmation is needed after a successful save.
         */
        if (saved) {
            dispose();
            return;
        }

        /*
         * If nothing was entered or changed, close normally.
         */
        if (!formChanged && formIsEmpty()) {
            dispose();
            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        existingRecord == null
                                ? "Discard this new RMA?\n\n"
                                  + "Any information entered "
                                  + "will be lost."
                                : "Discard your changes to RMA "
                                  + existingRecord.getRmaNumber()
                                  + "?\n\n"
                                  + "Any unsaved changes will be lost.",
                        "Unsaved Changes",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    private boolean formIsEmpty() {
        if (!rmaNumberField.getText().isBlank()) {
            return false;
        }

        if (!dateSentField.getText().isBlank()) {
            return false;
        }

        if (!dateReceivedField.getText().isBlank()) {
            return false;
        }

        if (!outgoingTrackingField.getText().isBlank()) {
            return false;
        }

        if (!returnTrackingField.getText().isBlank()) {
            return false;
        }

        if (!notesArea.getText().isBlank()) {
            return false;
        }

        for (int row = 0; row < shippingTableModel.getRowCount(); row++) {
            if (!getShippingValue(row, 0).isBlank() || !getShippingValue(row, 1).isBlank()) {
                return false;
            }
        }

        for (int row = 0;
             row < repairItemsTableModel.getRowCount();
             row++) {

            for (int column = 0;
                 column
                         < repairItemsTableModel
                         .getColumnCount();
                 column++) {

                if (!getTableValue(
                        row,
                        column
                ).isBlank()) {
                    return false;
                }
            }
        }

        return repairItemsTableModel.getRowCount() == 0;
    }

    private void stopTableEditing() {
        if (shippingTable.isEditing()) {
            shippingTable.getCellEditor().stopCellEditing();
        }
        if (repairItemsTable.isEditing()) {
            repairItemsTable
                    .getCellEditor()
                    .stopCellEditing();
        }
    }

    private void showValidationMessage(
            String message,
            String title,
            JComponent component
    ) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.WARNING_MESSAGE
        );

        component.requestFocusInWindow();

        if (component instanceof JTextField textField) {
            textField.selectAll();
        }
    }

    private void showDatabaseError(
            String message,
            SQLException exception
    ) {
        /*
         * Keep the message friendly for the user.
         * Print technical details in the console.
         */
        JOptionPane.showMessageDialog(
                this,
                message
                        + "\n\n"
                        + "Please verify that the database "
                        + "is available and try again.",
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );

        System.err.println(message);
        exception.printStackTrace();
    }

    private String formatDate(
            LocalDate date
    ) {
        if (date == null) {
            return "";
        }

        return date.toString();
    }

    private String emptyIfNull(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value;
    }
}