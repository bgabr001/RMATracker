package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.RmaSearchCriteria;
import com.harpenterprises.rmatracker.model.Status;
import com.harpenterprises.rmatracker.service.RmaSearchService;
import com.harpenterprises.rmatracker.storage.RmaRepository;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class MainWindow extends JFrame {

    /*
     * Search controls
     */
    private final JTextField searchField;
    private final JTextField dateFromField;
    private final JTextField dateToField;

    private final JComboBox<Object> statusFilterComboBox;
    private final JComboBox<String> countyFilterComboBox;
    private final JComboBox<String> machineFilterComboBox;

    private final JButton clearSearchButton;

    /*
     * Main RMA table
     */
    private final JTable rmaTable;
    private final DefaultTableModel rmaTableModel;
    private final TableRowSorter<DefaultTableModel>
            rmaTableSorter;

    /*
     * Repair-item table
     */
    private final JTable repairItemsTable;
    private final DefaultTableModel repairItemsTableModel;

    /*
     * Selected RMA details
     */
    private final JLabel selectedRmaNumberValue;
    private final JLabel selectedDateSentValue;
    private final JLabel selectedDateReceivedValue;
    private final JLabel selectedStatusValue;
    private final JLabel selectedOutgoingTrackingValue;
    private final JLabel selectedReturnTrackingValue;
    private final JTextArea selectedNotesArea;

    /*
     * Buttons
     */
    private final JButton newRmaButton;
    private final JButton openRmaButton;
    private final JButton statusHistoryButton;
    private final JButton deleteRmaButton;
    private final JButton refreshButton;
    private final JButton exitButton;

    /*
     * Messages
     */
    private final JLabel resultLabel;
    private final JLabel detailMessageLabel;

    /*
     * Data and services
     */
    private final RmaRepository repository;
    private final RmaSearchService searchService;

    private final List<RmaRecord> allRmaRecords;
    private final List<RmaRecord> displayedRmaRecords;

    /*
     * Prevents filter events while rebuilding
     * county and machine choices.
     */
    private boolean rebuildingFilterChoices;

    public MainWindow() {
        repository = new RmaRepository();
        searchService = new RmaSearchService();

        allRmaRecords = new ArrayList<>();
        displayedRmaRecords = new ArrayList<>();

        rebuildingFilterChoices = false;

        setTitle("RMA Repair Tracker");
        setSize(1450, 900);
        setMinimumSize(new Dimension(1100, 720));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        /*
         * Create search controls.
         */
        searchField = new JTextField(22);

        searchField.setToolTipText(
                "Search every RMA and repair-item field"
        );

        dateFromField = new JTextField(10);
        dateToField = new JTextField(10);

        dateFromField.setToolTipText(
                "Date Sent From (YYYY-MM-DD)"
        );

        dateToField.setToolTipText(
                "Date Sent To (YYYY-MM-DD)"
        );

        statusFilterComboBox = new JComboBox<>();
        countyFilterComboBox = new JComboBox<>();
        machineFilterComboBox = new JComboBox<>();

        statusFilterComboBox.addItem("All Statuses");

        for (Status status : Status.values()) {
            statusFilterComboBox.addItem(status);
        }

        countyFilterComboBox.addItem("All Counties");

        machineFilterComboBox.addItem(
                "All Machine Types"
        );

        clearSearchButton = new JButton("Clear");

        /*
         * Create buttons.
         */
        newRmaButton = new JButton("New RMA");
        openRmaButton = new JButton("Open RMA");
        statusHistoryButton = new JButton("Status History");
        deleteRmaButton = new JButton("Delete RMA");
        refreshButton = new JButton("Refresh");
        exitButton = new JButton("Exit");

        resultLabel = new JLabel(" ");

        detailMessageLabel = new JLabel(
                "Select an RMA above to view its details."
        );

        /*
         * Selected-RMA labels.
         */
        selectedRmaNumberValue = new JLabel("-");
        selectedDateSentValue = new JLabel("-");
        selectedDateReceivedValue = new JLabel("-");
        selectedStatusValue = new JLabel("-");
        selectedOutgoingTrackingValue = new JLabel("-");
        selectedReturnTrackingValue = new JLabel("-");

        selectedNotesArea = new JTextArea(4, 30);
        selectedNotesArea.setEditable(false);
        selectedNotesArea.setLineWrap(true);
        selectedNotesArea.setWrapStyleWord(true);

        selectedNotesArea.setBackground(
                UIManager.getColor("Panel.background")
        );

        /*
         * Main RMA table.
         */
        String[] rmaColumns = {
                "RMA Number",
                "Date Sent",
                "Date Received",
                "Status",
                "Outgoing Tracking",
                "Return Tracking",
                "Items",
                "Notes"
        };

        rmaTableModel = new DefaultTableModel(
                rmaColumns,
                0
        ) {
            @Override
            public boolean isCellEditable(
                    int row,
                    int column
            ) {
                return false;
            }
        };

        rmaTable = new JTable(rmaTableModel);

        rmaTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        rmaTable.setRowHeight(25);

        rmaTable.getTableHeader()
                .setReorderingAllowed(false);

        rmaTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF
        );

        rmaTableSorter =
                new TableRowSorter<>(rmaTableModel);

        rmaTable.setRowSorter(rmaTableSorter);

        configureRmaColumnWidths();

        /*
         * Repair-items table.
         */
        String[] repairColumns = {
                "County",
                "Machine Type",
                "Serial Number",
                "Version",
                "Problem Description",
                "Repair Description"
        };

        repairItemsTableModel =
                new DefaultTableModel(
                        repairColumns,
                        0
                ) {
                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {
                        return false;
                    }
                };

        repairItemsTable =
                new JTable(repairItemsTableModel);

        repairItemsTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        repairItemsTable.setRowHeight(25);

        repairItemsTable.getTableHeader()
                .setReorderingAllowed(false);

        repairItemsTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF
        );

        configureRepairItemColumnWidths();

        /*
         * Highlight matching cells.
         */
        MatchingCellRenderer matchingRenderer =
                new MatchingCellRenderer();

        rmaTable.setDefaultRenderer(
                Object.class,
                matchingRenderer
        );

        repairItemsTable.setDefaultRenderer(
                Object.class,
                matchingRenderer
        );

        /*
         * Window layout.
         */
        setLayout(new BorderLayout(10, 10));

        add(
                createHeaderPanel(),
                BorderLayout.NORTH
        );

        add(
                createCenterPanel(),
                BorderLayout.CENTER
        );

        add(
                createBottomPanel(),
                BorderLayout.SOUTH
        );

        addListeners();
        addKeyboardShortcuts();

        setDetailControlsEnabled(false);

        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(
                            WindowEvent event
                    ) {
                        exitApplication();
                    }
                }
        );

        refreshRmaTable(null);
    }

    private JPanel createHeaderPanel() {
        JPanel outerPanel =
                new JPanel(new BorderLayout(10, 10));

        outerPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        12,
                        15,
                        0,
                        15
                )
        );

        JLabel titleLabel =
                new JLabel("RMA Repair Tracker");

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        26
                )
        );

        JPanel firstFilterRow =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        firstFilterRow.add(new JLabel("Search:"));
        firstFilterRow.add(searchField);
        firstFilterRow.add(clearSearchButton);

        firstFilterRow.add(
                Box.createHorizontalStrut(10)
        );

        firstFilterRow.add(new JLabel("Status:"));
        firstFilterRow.add(statusFilterComboBox);

        JPanel secondFilterRow =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        secondFilterRow.add(new JLabel("County:"));
        secondFilterRow.add(countyFilterComboBox);

        secondFilterRow.add(
                Box.createHorizontalStrut(8)
        );

        secondFilterRow.add(new JLabel("Machine:"));
        secondFilterRow.add(machineFilterComboBox);

        secondFilterRow.add(
                Box.createHorizontalStrut(8)
        );

        secondFilterRow.add(new JLabel("Sent From:"));
        secondFilterRow.add(dateFromField);

        secondFilterRow.add(new JLabel("To:"));
        secondFilterRow.add(dateToField);

        JPanel filtersPanel = new JPanel();

        filtersPanel.setLayout(
                new BoxLayout(
                        filtersPanel,
                        BoxLayout.Y_AXIS
                )
        );

        filtersPanel.add(firstFilterRow);
        filtersPanel.add(secondFilterRow);

        outerPanel.add(
                titleLabel,
                BorderLayout.WEST
        );

        outerPanel.add(
                filtersPanel,
                BorderLayout.EAST
        );

        return outerPanel;
    }

    private JSplitPane createCenterPanel() {
        JPanel rmaPanel =
                new JPanel(new BorderLayout());

        rmaPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "RMA Records"
                )
        );

        rmaPanel.add(
                new JScrollPane(rmaTable),
                BorderLayout.CENTER
        );

        JSplitPane splitPane =
                new JSplitPane(
                        JSplitPane.VERTICAL_SPLIT,
                        rmaPanel,
                        createDetailPanel()
                );

        splitPane.setResizeWeight(0.50);
        splitPane.setDividerLocation(350);

        splitPane.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        15,
                        5,
                        15
                )
        );

        return splitPane;
    }

    private JPanel createDetailPanel() {
        JPanel panel =
                new JPanel(new BorderLayout(10, 10));

        panel.setBorder(
                BorderFactory.createTitledBorder(
                        "Selected RMA Details"
                )
        );

        detailMessageLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        8,
                        5,
                        8
                )
        );

        panel.add(
                detailMessageLabel,
                BorderLayout.NORTH
        );

        JPanel repairPanel =
                new JPanel(new BorderLayout());

        repairPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Repair Items"
                )
        );

        repairPanel.add(
                new JScrollPane(repairItemsTable),
                BorderLayout.CENTER
        );

        JSplitPane detailSplitPane =
                new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        createRmaInformationPanel(),
                        repairPanel
                );

        detailSplitPane.setResizeWeight(0.32);
        detailSplitPane.setDividerLocation(390);

        panel.add(
                detailSplitPane,
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel createRmaInformationPanel() {
        JPanel panel =
                new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createTitledBorder(
                        "RMA Information"
                )
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.insets =
                new Insets(5, 8, 5, 8);

        constraints.anchor =
                GridBagConstraints.NORTHWEST;

        int row = 0;

        addDetailRow(
                panel,
                constraints,
                row++,
                "RMA Number:",
                selectedRmaNumberValue
        );

        addDetailRow(
                panel,
                constraints,
                row++,
                "Date Sent:",
                selectedDateSentValue
        );

        addDetailRow(
                panel,
                constraints,
                row++,
                "Date Received:",
                selectedDateReceivedValue
        );

        addDetailRow(
                panel,
                constraints,
                row++,
                "Status:",
                selectedStatusValue
        );

        addDetailRow(
                panel,
                constraints,
                row++,
                "Outgoing Tracking:",
                selectedOutgoingTrackingValue
        );

        addDetailRow(
                panel,
                constraints,
                row++,
                "Return Tracking:",
                selectedReturnTrackingValue
        );

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;

        JLabel notesLabel =
                new JLabel("Notes:");

        notesLabel.setFont(
                notesLabel.getFont()
                        .deriveFont(Font.BOLD)
        );

        panel.add(notesLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;

        panel.add(
                new JScrollPane(selectedNotesArea),
                constraints
        );

        return panel;
    }

    private void addDetailRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String labelText,
            JLabel valueLabel
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;

        JLabel label =
                new JLabel(labelText);

        label.setFont(
                label.getFont()
                        .deriveFont(Font.BOLD)
        );

        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        panel.add(valueLabel, constraints);
    }

    private JPanel createBottomPanel() {
        JPanel outerPanel =
                new JPanel(new BorderLayout());

        outerPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        15,
                        10,
                        15
                )
        );

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        buttons.add(newRmaButton);
        buttons.add(openRmaButton);
        buttons.add(statusHistoryButton);
        buttons.add(deleteRmaButton);
        buttons.add(refreshButton);
        buttons.add(exitButton);

        outerPanel.add(
                buttons,
                BorderLayout.WEST
        );

        outerPanel.add(
                resultLabel,
                BorderLayout.EAST
        );

        return outerPanel;
    }

    private void configureRmaColumnWidths() {
        int[] widths = {
                120,
                120,
                130,
                140,
                180,
                180,
                60,
                350
        };

        for (int index = 0;
             index < widths.length;
             index++) {

            rmaTable.getColumnModel()
                    .getColumn(index)
                    .setPreferredWidth(widths[index]);
        }
    }

    private void configureRepairItemColumnWidths() {
        int[] widths = {
                130,
                140,
                150,
                100,
                280,
                280
        };

        for (int index = 0;
             index < widths.length;
             index++) {

            repairItemsTable.getColumnModel()
                    .getColumn(index)
                    .setPreferredWidth(widths[index]);
        }
    }

    private void addListeners() {
        DocumentListener filterDocumentListener =
                new DocumentListener() {

                    @Override
                    public void insertUpdate(
                            DocumentEvent event
                    ) {
                        applySearch(null);
                    }

                    @Override
                    public void removeUpdate(
                            DocumentEvent event
                    ) {
                        applySearch(null);
                    }

                    @Override
                    public void changedUpdate(
                            DocumentEvent event
                    ) {
                        applySearch(null);
                    }
                };

        searchField.getDocument()
                .addDocumentListener(
                        filterDocumentListener
                );

        dateFromField.getDocument()
                .addDocumentListener(
                        filterDocumentListener
                );

        dateToField.getDocument()
                .addDocumentListener(
                        filterDocumentListener
                );

        statusFilterComboBox.addActionListener(
                event -> filterChoiceChanged()
        );

        countyFilterComboBox.addActionListener(
                event -> filterChoiceChanged()
        );

        machineFilterComboBox.addActionListener(
                event -> filterChoiceChanged()
        );

        clearSearchButton.addActionListener(
                event -> clearAllFilters()
        );

        newRmaButton.addActionListener(
                event -> openNewRmaWindow()
        );

        openRmaButton.addActionListener(
                event -> openSelectedRma()
        );

        statusHistoryButton.addActionListener(
                event -> openSelectedRmaHistory()
        );

        deleteRmaButton.addActionListener(
                event -> deleteSelectedRma()
        );

        refreshButton.addActionListener(
                event -> {
                    RmaRecord selected =
                            getSelectedRma();

                    String selectedNumber =
                            selected == null
                                    ? null
                                    : selected.getRmaNumber();

                    refreshRmaTable(selectedNumber);
                }
        );

        exitButton.addActionListener(
                event -> exitApplication()
        );

        rmaTable.getSelectionModel()
                .addListSelectionListener(
                        this::rmaSelectionChanged
                );

        /*
         * Double-click top table to open the RMA.
         */
        rmaTable.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(
                            MouseEvent event
                    ) {
                        if (event.getClickCount() == 2
                                && SwingUtilities
                                .isLeftMouseButton(event)) {

                            openSelectedRma();
                        }
                    }
                }
        );

        /*
         * Double-click repair item to open its RMA.
         */
        repairItemsTable.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(
                            MouseEvent event
                    ) {
                        if (event.getClickCount() == 2
                                && SwingUtilities
                                .isLeftMouseButton(event)) {

                            openSelectedRma();
                        }
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
         * Enter opens the selected RMA unless the
         * user is editing one of the search fields.
         */
        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_ENTER,
                        0
                ),
                "openSelectedRma"
        );

        actionMap.put(
                "openSelectedRma",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent event
                    ) {
                        Component focusOwner =
                                KeyboardFocusManager
                                        .getCurrentKeyboardFocusManager()
                                        .getFocusOwner();

                        if (focusOwner == searchField
                                || focusOwner == dateFromField
                                || focusOwner == dateToField) {

                            return;
                        }

                        if (getSelectedRma() != null) {
                            openSelectedRma();
                        }
                    }
                }
        );

        /*
         * Escape clears all filters.
         */
        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_ESCAPE,
                        0
                ),
                "clearAllFilters"
        );

        actionMap.put(
                "clearAllFilters",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent event
                    ) {
                        clearAllFilters();
                    }
                }
        );
    }

    private void filterChoiceChanged() {
        if (rebuildingFilterChoices) {
            return;
        }

        applySearch(null);
    }

    private void clearAllFilters() {
        searchField.setText("");
        dateFromField.setText("");
        dateToField.setText("");

        statusFilterComboBox.setSelectedIndex(0);
        countyFilterComboBox.setSelectedIndex(0);
        machineFilterComboBox.setSelectedIndex(0);

        searchField.requestFocusInWindow();

        applySearch(null);
    }

    private Status getSelectedStatus() {
        Object selected =
                statusFilterComboBox.getSelectedItem();

        if (selected instanceof Status) {
            return (Status) selected;
        }

        return null;
    }

    private String getSelectedCounty() {
        Object selected =
                countyFilterComboBox.getSelectedItem();

        if (selected == null
                || selected.equals("All Counties")) {

            return null;
        }

        return selected.toString();
    }

    private String getSelectedMachineType() {
        Object selected =
                machineFilterComboBox.getSelectedItem();

        if (selected == null
                || selected.equals(
                "All Machine Types"
        )) {
            return null;
        }

        return selected.toString();
    }

    private LocalDate parseFilterDate(
            JTextField field
    ) {
        String text =
                field.getText().trim();

        if (text.isEmpty()) {
            field.setBackground(
                    UIManager.getColor(
                            "TextField.background"
                    )
            );

            return null;
        }

        try {
            LocalDate date =
                    LocalDate.parse(text);

            field.setBackground(
                    UIManager.getColor(
                            "TextField.background"
                    )
            );

            return date;

        } catch (DateTimeParseException exception) {
            /*
             * Light red background signals an invalid date.
             */
            field.setBackground(
                    new Color(255, 220, 220)
            );

            return null;
        }
    }

    private RmaSearchCriteria createSearchCriteria() {
        return new RmaSearchCriteria(
                searchField.getText(),
                getSelectedStatus(),
                getSelectedCounty(),
                getSelectedMachineType(),
                parseFilterDate(dateFromField),
                parseFilterDate(dateToField)
        );
    }

    private void applySearch(
            String preferredRmaNumber
    ) {
        RmaSearchCriteria criteria =
                createSearchCriteria();

        displayedRmaRecords.clear();

        displayedRmaRecords.addAll(
                searchService.search(
                        allRmaRecords,
                        criteria
                )
        );

        populateRmaTable();

        rmaTable.clearSelection();
        clearSelectedRmaDetails();

        /*
         * Restore the previously selected RMA when possible.
         */
        if (preferredRmaNumber != null) {
            selectRmaByNumber(preferredRmaNumber);
        }

        /*
         * While searching/filtering, automatically select
         * the first match if no preserved RMA was found.
         */
        if (rmaTable.getSelectedRow() == -1
                && filtersAreActive()
                && !displayedRmaRecords.isEmpty()) {

            int firstViewRow =
                    rmaTable.convertRowIndexToView(0);

            if (firstViewRow >= 0) {
                rmaTable.setRowSelectionInterval(
                        firstViewRow,
                        firstViewRow
                );
            }
        }

        updateResultLabel();

        rmaTable.repaint();
        repairItemsTable.repaint();
    }

    private boolean filtersAreActive() {
        return !searchField.getText().trim().isEmpty()
                || getSelectedStatus() != null
                || getSelectedCounty() != null
                || getSelectedMachineType() != null
                || !dateFromField
                .getText()
                .trim()
                .isEmpty()
                || !dateToField
                .getText()
                .trim()
                .isEmpty();
    }

    private void populateRmaTable() {
        rmaTableModel.setRowCount(0);

        for (RmaRecord record :
                displayedRmaRecords) {

            int itemCount =
                    record.getRepairItems() == null
                            ? 0
                            : record.getRepairItems().size();

            rmaTableModel.addRow(
                    new Object[]{
                            record.getRmaNumber(),
                            formatValue(
                                    record.getDateSent()
                            ),
                            formatValue(
                                    record.getDateReceived()
                            ),
                            formatValue(
                                    record.getStatus()
                            ),
                            displayString(
                                    record
                                            .getOutgoingTrackingNumber()
                            ),
                            displayString(
                                    record
                                            .getReturnTrackingNumber()
                            ),
                            itemCount,
                            displayString(
                                    record.getNotes()
                            )
                    }
            );
        }
    }

    private void rmaSelectionChanged(
            ListSelectionEvent event
    ) {
        if (event.getValueIsAdjusting()) {
            return;
        }

        RmaRecord selectedRecord =
                getSelectedRma();

        if (selectedRecord == null) {
            clearSelectedRmaDetails();
            return;
        }

        showSelectedRmaDetails(selectedRecord);
    }

    private void showSelectedRmaDetails(
            RmaRecord record
    ) {
        detailMessageLabel.setText(
                "Viewing "
                        + record.getRmaNumber()
                        + ". Double-click the top row or press Enter to open."
        );

        selectedRmaNumberValue.setText(
                displayString(record.getRmaNumber())
        );

        selectedDateSentValue.setText(
                formatValue(record.getDateSent())
        );

        selectedDateReceivedValue.setText(
                formatValue(record.getDateReceived())
        );

        selectedStatusValue.setText(
                formatValue(record.getStatus())
        );

        selectedOutgoingTrackingValue.setText(
                displayString(
                        record.getOutgoingTrackingNumber()
                )
        );

        selectedReturnTrackingValue.setText(
                displayString(
                        record.getReturnTrackingNumber()
                )
        );

        selectedNotesArea.setText(
                displayString(record.getNotes())
        );

        populateRepairItemsTable(record);

        setDetailControlsEnabled(true);
    }

    private void populateRepairItemsTable(
            RmaRecord record
    ) {
        repairItemsTableModel.setRowCount(0);

        List<RepairItem> items =
                record.getRepairItems();

        if (items == null) {
            return;
        }

        String selectedCounty =
                normalize(getSelectedCounty());

        String selectedMachine =
                normalize(getSelectedMachineType());

        String freeText =
                normalize(searchField.getText());

        boolean rmaLevelMatches =
                rmaFieldsContainAllWords(
                        record,
                        freeText
                );

        for (RepairItem item : items) {
            if (!selectedCounty.isEmpty()
                    && !normalize(item.getCounty())
                    .equals(selectedCounty)) {

                continue;
            }

            if (!selectedMachine.isEmpty()
                    && !normalize(
                    item.getMachineType()
            ).equals(selectedMachine)) {

                continue;
            }

            /*
             * If the free-text search matched an RMA-level
             * field, show all filtered repair items.
             *
             * Otherwise, show only matching repair items.
             */
            if (!freeText.isEmpty()
                    && !rmaLevelMatches
                    && !repairItemContainsAllWords(
                    item,
                    freeText
            )) {
                continue;
            }

            addRepairItemToTable(item);
        }
    }

    private void addRepairItemToTable(
            RepairItem item
    ) {
        repairItemsTableModel.addRow(
                new Object[]{
                        displayString(item.getCounty()),
                        displayString(item.getMachineType()),
                        displayString(item.getSerialNumber()),
                        displayString(item.getVersion()),
                        displayString(
                                item.getProblemDescription()
                        ),
                        displayString(
                                item.getRepairDescription()
                        )
                }
        );
    }

    private boolean rmaFieldsContainAllWords(
            RmaRecord record,
            String searchText
    ) {
        String text =
                buildSearchableText(
                        record.getRmaNumber(),
                        record.getDateSent(),
                        record.getDateReceived(),
                        record.getStatus(),
                        record.getOutgoingTrackingNumber(),
                        record.getReturnTrackingNumber(),
                        record.getNotes()
                );

        return containsAllWords(text, searchText);
    }

    private boolean repairItemContainsAllWords(
            RepairItem item,
            String searchText
    ) {
        String text =
                buildSearchableText(
                        item.getCounty(),
                        item.getMachineType(),
                        item.getSerialNumber(),
                        item.getVersion(),
                        item.getProblemDescription(),
                        item.getRepairDescription()
                );

        return containsAllWords(text, searchText);
    }

    private String buildSearchableText(
            Object... values
    ) {
        StringBuilder text =
                new StringBuilder();

        for (Object value : values) {
            if (value != null) {
                text.append(value)
                        .append(' ');
            }
        }

        return normalize(text.toString());
    }

    private boolean containsAllWords(
            String searchableText,
            String searchText
    ) {
        if (searchText.isEmpty()) {
            return true;
        }

        String[] words =
                searchText.split("\\s+");

        for (String word : words) {
            if (!searchableText.contains(word)) {
                return false;
            }
        }

        return true;
    }

    private void rebuildFilterChoices() {
        rebuildingFilterChoices = true;

        String previousCounty =
                getSelectedCounty();

        String previousMachine =
                getSelectedMachineType();

        Set<String> counties =
                new TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        Set<String> machineTypes =
                new TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        for (RmaRecord record : allRmaRecords) {
            List<RepairItem> items =
                    record.getRepairItems();

            if (items == null) {
                continue;
            }

            for (RepairItem item : items) {
                if (item.getCounty() != null
                        && !item.getCounty().isBlank()) {

                    counties.add(
                            item.getCounty().trim()
                    );
                }

                if (item.getMachineType() != null
                        && !item
                        .getMachineType()
                        .isBlank()) {

                    machineTypes.add(
                            item
                                    .getMachineType()
                                    .trim()
                    );
                }
            }
        }

        countyFilterComboBox.removeAllItems();
        countyFilterComboBox.addItem("All Counties");

        for (String county : counties) {
            countyFilterComboBox.addItem(county);
        }

        machineFilterComboBox.removeAllItems();

        machineFilterComboBox.addItem(
                "All Machine Types"
        );

        for (String machineType : machineTypes) {
            machineFilterComboBox.addItem(machineType);
        }

        restoreComboSelection(
                countyFilterComboBox,
                previousCounty
        );

        restoreComboSelection(
                machineFilterComboBox,
                previousMachine
        );

        rebuildingFilterChoices = false;
    }

    private void restoreComboSelection(
            JComboBox<String> comboBox,
            String requestedValue
    ) {
        if (requestedValue == null) {
            comboBox.setSelectedIndex(0);
            return;
        }

        for (int index = 0;
             index < comboBox.getItemCount();
             index++) {

            String item =
                    comboBox.getItemAt(index);

            if (requestedValue.equalsIgnoreCase(item)) {
                comboBox.setSelectedIndex(index);
                return;
            }
        }

        comboBox.setSelectedIndex(0);
    }

    private void selectRmaByNumber(
            String rmaNumber
    ) {
        if (rmaNumber == null) {
            return;
        }

        for (int modelRow = 0;
             modelRow < displayedRmaRecords.size();
             modelRow++) {

            RmaRecord record =
                    displayedRmaRecords.get(modelRow);

            if (!rmaNumber.equals(
                    record.getRmaNumber()
            )) {
                continue;
            }

            int viewRow =
                    rmaTable.convertRowIndexToView(
                            modelRow
                    );

            if (viewRow >= 0) {
                rmaTable.setRowSelectionInterval(
                        viewRow,
                        viewRow
                );

                rmaTable.scrollRectToVisible(
                        rmaTable.getCellRect(
                                viewRow,
                                0,
                                true
                        )
                );
            }

            return;
        }
    }

    private void clearSelectedRmaDetails() {
        if (displayedRmaRecords.isEmpty()
                && filtersAreActive()) {

            detailMessageLabel.setText(
                    "No matching RMAs were found."
            );

        } else {
            detailMessageLabel.setText(
                    "Select an RMA above to view its details."
            );
        }

        selectedRmaNumberValue.setText("-");
        selectedDateSentValue.setText("-");
        selectedDateReceivedValue.setText("-");
        selectedStatusValue.setText("-");
        selectedOutgoingTrackingValue.setText("-");
        selectedReturnTrackingValue.setText("-");

        selectedNotesArea.setText("");

        repairItemsTableModel.setRowCount(0);

        setDetailControlsEnabled(false);
    }

    private void setDetailControlsEnabled(
            boolean enabled
    ) {
        openRmaButton.setEnabled(enabled);
        statusHistoryButton.setEnabled(enabled);
        deleteRmaButton.setEnabled(enabled);
        repairItemsTable.setEnabled(enabled);
    }

    private RmaRecord getSelectedRma() {
        int selectedViewRow =
                rmaTable.getSelectedRow();

        if (selectedViewRow == -1) {
            return null;
        }

        int selectedModelRow =
                rmaTable.convertRowIndexToModel(
                        selectedViewRow
                );

        if (selectedModelRow < 0
                || selectedModelRow
                >= displayedRmaRecords.size()) {

            return null;
        }

        return displayedRmaRecords.get(
                selectedModelRow
        );
    }

    private void openNewRmaWindow() {
        RmaFormDialog dialog =
                new RmaFormDialog(this);

        dialog.setVisible(true);

        if (dialog.isSaved()) {
            refreshRmaTable(null);
        }
    }

    private void openSelectedRma() {
        RmaRecord selectedRecord =
                getSelectedRma();

        if (selectedRecord == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an RMA to open.",
                    "No RMA Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        String rmaNumber =
                selectedRecord.getRmaNumber();

        RmaFormDialog dialog =
                new RmaFormDialog(
                        this,
                        selectedRecord
                );

        dialog.setVisible(true);

        if (dialog.isSaved()) {
            refreshRmaTable(rmaNumber);
        }
    }

    private void openSelectedRmaHistory() {
        RmaRecord selectedRecord =
                getSelectedRma();

        if (selectedRecord == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an RMA to view its status history.",
                    "No RMA Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        StatusHistoryDialog dialog =
                new StatusHistoryDialog(
                        this,
                        selectedRecord.getRmaNumber()
                );

        dialog.setVisible(true);
    }

    private void deleteSelectedRma() {
        RmaRecord selectedRecord =
                getSelectedRma();

        if (selectedRecord == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an RMA to delete.",
                    "No RMA Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        String rmaNumber =
                selectedRecord.getRmaNumber();

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to permanently "
                                + "delete RMA "
                                + rmaNumber
                                + "?\n\n"
                                + "Its repair items and status "
                                + "history will also be deleted.\n"
                                + "This action cannot be undone.",
                        "Delete RMA",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean deleted =
                    repository.delete(rmaNumber);

            if (!deleted) {
                JOptionPane.showMessageDialog(
                        this,
                        "RMA "
                                + rmaNumber
                                + " could not be found.\n\n"
                                + "It may have already been deleted.",
                        "RMA Not Found",
                        JOptionPane.WARNING_MESSAGE
                );

                refreshRmaTable(null);
                return;
            }

            refreshRmaTable(null);

            JOptionPane.showMessageDialog(
                    this,
                    "RMA "
                            + rmaNumber
                            + " was deleted successfully.",
                    "RMA Deleted",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (SQLException exception) {
            showDatabaseError(
                    "The RMA could not be deleted.",
                    exception
            );
        }
    }

    private void refreshRmaTable(
            String preferredRmaNumber
    ) {
        try {
            allRmaRecords.clear();

            allRmaRecords.addAll(
                    repository.findAll()
            );

            rebuildFilterChoices();

            applySearch(preferredRmaNumber);

        } catch (SQLException exception) {
            showDatabaseError(
                    "The RMA records could not be loaded.",
                    exception
            );
        }
    }

    private void updateResultLabel() {
        if (displayedRmaRecords.isEmpty()
                && filtersAreActive()) {

            resultLabel.setText(
                    "No matching RMAs | 0 of "
                            + allRmaRecords.size()
            );

            return;
        }

        resultLabel.setText(
                "Showing "
                        + displayedRmaRecords.size()
                        + " of "
                        + allRmaRecords.size()
                        + " RMAs"
        );
    }

    private void showDatabaseError(
            String message,
            SQLException exception
    ) {
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

    private String normalize(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String formatValue(
            Object value
    ) {
        if (value == null) {
            return "-";
        }

        return value.toString();
    }

    private String displayString(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }

    private void exitApplication() {
        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to exit?",
                        "Exit RMA Tracker",
                        JOptionPane.YES_NO_OPTION
                );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    /**
     * Highlights cells containing the free-text search.
     *
     * Selected rows keep the normal selection color.
     */
    private class MatchingCellRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component component =
                    super.getTableCellRendererComponent(
                            table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column
                    );

            if (isSelected) {
                return component;
            }

            String searchText =
                    normalize(searchField.getText());

            String cellText =
                    value == null
                            ? ""
                            : normalize(value.toString());

            if (!searchText.isEmpty()
                    && cellContainsSearchWord(
                    cellText,
                    searchText
            )) {
                component.setBackground(
                        new Color(255, 245, 170)
                );
            } else {
                component.setBackground(
                        table.getBackground()
                );
            }

            return component;
        }

        private boolean cellContainsSearchWord(
                String cellText,
                String searchText
        ) {
            String[] words =
                    searchText.split("\\s+");

            for (String word : words) {
                if (cellText.contains(word)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window =
                    new MainWindow();

            window.setVisible(true);
        });
    }
}