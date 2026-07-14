package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.RmaSearchCriteria;
import com.harpenterprises.rmatracker.model.ShippingInfo;
import com.harpenterprises.rmatracker.model.Status;
import com.harpenterprises.rmatracker.service.BackupService;
import com.harpenterprises.rmatracker.service.ExcelExportService;
import com.harpenterprises.rmatracker.service.RmaSearchService;
import com.harpenterprises.rmatracker.storage.RmaRepository;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final JTextArea selectedSentShippingValue;
    private final JTextArea selectedReturnShippingValue;
    private final JTextArea selectedNotesArea;

    /*
     * Buttons
     */
    private final JButton newRmaButton;
    private final JButton openRmaButton;
    private final JButton viewReportButton;
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
    private final BackupService backupService;
    private final ExcelExportService excelExportService;

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
        backupService = new BackupService();
        excelExportService = new ExcelExportService();

        allRmaRecords = new ArrayList<>();
        displayedRmaRecords = new ArrayList<>();

        rebuildingFilterChoices = false;

        setTitle("RMA Tracker");
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
        viewReportButton = new JButton("View RMA Report");
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
        selectedSentShippingValue = createReadOnlyTextArea(3, 22);
        selectedReturnShippingValue = createReadOnlyTextArea(3, 22);

        selectedSentShippingValue.setText("-");
        selectedReturnShippingValue.setText("-");

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
                "Date Sent (YYYY-MM-DD)",
                "Date Received (YYYY-MM-DD)",
                "Status",
                "Sent Shipping",
                "Return Shipping",
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
                "Repair Description",
                "Received"
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
         * Shipping columns use a multiline renderer so every shipment
         * appears on its own line.
         */
        MultilineTableCellRenderer shippingRenderer =
                new MultilineTableCellRenderer();

        rmaTable.getColumnModel()
                .getColumn(4)
                .setCellRenderer(shippingRenderer);

        rmaTable.getColumnModel()
                .getColumn(5)
                .setCellRenderer(shippingRenderer);

        /*
         * Window layout.
         */
        setLayout(new BorderLayout(10, 10));
        setJMenuBar(
                new MainWindowMenuBar(
                        this::backupDatabase,
                        this::restoreDatabase,
                        this::exitApplication,
                        this::openSelectedRmaReport,
                        this::exportSelectedRmaToExcel,
                        this::exportDisplayedRmasToExcel
                )
        );

        add(
                new MainWindowSearchPanel(
                        searchField,
                        clearSearchButton,
                        statusFilterComboBox,
                        countyFilterComboBox,
                        machineFilterComboBox,
                        dateFromField,
                        dateToField
                ),
                BorderLayout.NORTH
        );

        add(
                createCenterPanel(),
                BorderLayout.CENTER
        );

        add(
                new MainWindowActionBar(
                        newRmaButton,
                        openRmaButton,
                        viewReportButton,
                        statusHistoryButton,
                        deleteRmaButton,
                        refreshButton,
                        exitButton,
                        resultLabel
                ),
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

    private JSplitPane createCenterPanel() {
        JPanel rmaPanel = createTablePanel("RMA Records", rmaTable);

        JSplitPane splitPane =
                new JSplitPane(
                        JSplitPane.VERTICAL_SPLIT,
                        rmaPanel,
                        new MainWindowDetailsPanel(
                                detailMessageLabel,
                                selectedRmaNumberValue,
                                selectedDateSentValue,
                                selectedDateReceivedValue,
                                selectedStatusValue,
                                selectedSentShippingValue,
                                selectedReturnShippingValue,
                                selectedNotesArea,
                                repairItemsTable
                        )
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

    private JPanel createTablePanel(
            String title,
            JTable table
    ) {
        JPanel panel =
                new JPanel(new BorderLayout());

        panel.setBorder(
                BorderFactory.createTitledBorder(title)
        );

        panel.add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );

        return panel;
    }

    private JTextArea createReadOnlyTextArea(
            int rows,
            int columns
    ) {
        JTextArea textArea =
                new JTextArea(rows, columns);

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setBackground(
                UIManager.getColor("Panel.background")
        );

        return textArea;
    }

    private JScrollPane createShippingScrollPane(
            JTextArea textArea
    ) {
        JScrollPane scrollPane =
                new JScrollPane(textArea);

        scrollPane.setPreferredSize(
                new Dimension(230, 62)
        );

        return scrollPane;
    }

    private void configureRmaColumnWidths() {
        int[] widths = {
                120,
                150,
                170,
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
                280,
                90
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

        viewReportButton.addActionListener(
                event -> openSelectedRmaReport()
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
                        if (event.getClickCount() != 2
                                || !SwingUtilities
                                .isLeftMouseButton(event)) {
                            return;
                        }

                        int clickedViewRow =
                                rmaTable.rowAtPoint(
                                        event.getPoint()
                                );

                        if (clickedViewRow < 0) {
                            return;
                        }

                        /*
                         * Explicitly select the row under the mouse
                         * before opening it. This keeps double-click
                         * reliable even when the table is sorted or
                         * filtered.
                         */
                        rmaTable.setRowSelectionInterval(
                                clickedViewRow,
                                clickedViewRow
                        );

                        openSelectedRma();
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
                            formatShippingInformation(
                                    record,
                                    false
                            ),
                            formatShippingInformation(
                                    record,
                                    true
                            ),
                            itemCount,
                            displayString(
                                    record.getNotes()
                            )
                    }
            );

            updateRmaRowHeight(
                    rmaTableModel.getRowCount() - 1,
                    record
            );
        }
    }

    private void updateRmaRowHeight(
            int modelRow,
            RmaRecord record
    ) {
        int sentLines = countDisplayLines(
                formatShippingInformation(
                        record,
                        false
                )
        );

        int returnLines = countDisplayLines(
                formatShippingInformation(
                        record,
                        true
                )
        );

        int lineCount =
                Math.max(sentLines, returnLines);

        int preferredHeight =
                Math.max(25, 20 * lineCount + 6);

        int viewRow =
                rmaTable.convertRowIndexToView(modelRow);

        if (viewRow >= 0) {
            rmaTable.setRowHeight(
                    viewRow,
                    preferredHeight
            );
        }
    }

    private int countDisplayLines(String value) {
        if (value == null
                || value.isBlank()
                || "-".equals(value.trim())) {
            return 1;
        }

        return value.split("\\R", -1).length;
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

        selectedSentShippingValue.setText(
                formatShippingInformation(
                        record,
                        false
                )
        );

        selectedReturnShippingValue.setText(
                formatShippingInformation(
                        record,
                        true
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
                        ),
                        item.isReceived() ? "X" : ""
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
                        formatShippingInformation(record, false),
                        formatShippingInformation(record, true),
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
                        item.getRepairDescription(),
                        item.isReceived() ? "received" : "not received"
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
        selectedSentShippingValue.setText("-");
        selectedReturnShippingValue.setText("-");

        selectedNotesArea.setText("");

        repairItemsTableModel.setRowCount(0);

        setDetailControlsEnabled(false);
    }

    private void setDetailControlsEnabled(
            boolean enabled
    ) {
        openRmaButton.setEnabled(enabled);
        viewReportButton.setEnabled(enabled);
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

    private void openSelectedRmaReport() {
        RmaRecord selectedRecord =
                getSelectedRma();

        if (selectedRecord == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an RMA to view its report.",
                    "No RMA Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        RmaReportPreviewWindow reportWindow =
                new RmaReportPreviewWindow(
                        this,
                        selectedRecord
                );

        reportWindow.setVisible(true);
    }

    private void exportSelectedRmaToExcel() {
        RmaRecord selectedRecord = getSelectedRma();

        if (selectedRecord == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an RMA to export.",
                    "No RMA Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String rmaNumber = selectedRecord.getRmaNumber();
        String safeName = createSafeExcelFileName(rmaNumber);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Selected RMA to Excel");
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

        String timestamp =
                LocalDateTime.now().format(formatter);

        fileChooser.setSelectedFile(
                new File(
                        safeName
                                + "_"
                                + timestamp
                                + ".xlsx"
                )
        );
        fileChooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Excel Workbook (*.xlsx)",
                        "xlsx"
                )
        );

        int choice = fileChooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (!selectedFile.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            selectedFile = new File(
                    selectedFile.getParentFile(),
                    selectedFile.getName() + ".xlsx"
            );
        }

        if (selectedFile.exists()) {
            int overwriteChoice = JOptionPane.showConfirmDialog(
                    this,
                    "The file already exists.\n\n"
                            + selectedFile.getName()
                            + "\n\nReplace it?",
                    "Replace Existing File",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (overwriteChoice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Path outputPath = selectedFile.toPath();
            excelExportService.exportSelectedRma(selectedRecord, outputPath);

            int machineCount = selectedRecord.getRepairItems() == null
                    ? 0
                    : selectedRecord.getRepairItems().size();

            JOptionPane.showMessageDialog(
                    this,
                    "Excel export complete.\n\n"
                            + "RMA: " + valueOrBlank(rmaNumber) + "\n"
                            + "Machines exported: " + machineCount + "\n\n"
                            + "Saved to:\n" + outputPath.toAbsolutePath(),
                    "Excel Export Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The Excel file could not be created.\n\n"
                            + exception.getMessage(),
                    "Excel Export Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void exportDisplayedRmasToExcel() {
        if (displayedRmaRecords.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "There are no displayed RMAs to export.",
                    "Nothing to Export",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Displayed RMAs to Excel");
        fileChooser.setSelectedFile(
                new File(
                        "Displayed_RMAs_"
                                + LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")
                        )
                                + ".xlsx"
                )
        );
        fileChooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Excel Workbook (*.xlsx)",
                        "xlsx"
                )
        );

        int choice = fileChooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (!selectedFile.getName()
                .toLowerCase(Locale.ROOT)
                .endsWith(".xlsx")) {
            selectedFile = new File(
                    selectedFile.getParentFile(),
                    selectedFile.getName() + ".xlsx"
            );
        }

        if (selectedFile.exists()) {
            int overwriteChoice =
                    JOptionPane.showConfirmDialog(
                            this,
                            "The file already exists.\n\n"
                                    + selectedFile.getName()
                                    + "\n\nReplace it?",
                            "Replace Existing File",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

            if (overwriteChoice
                    != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Path outputPath = selectedFile.toPath();

            excelExportService.exportDisplayedRmas(
                    new ArrayList<>(displayedRmaRecords),
                    outputPath
            );

            int machineCount = 0;
            for (RmaRecord record : displayedRmaRecords) {
                if (record != null
                        && record.getRepairItems() != null) {
                    machineCount += record
                            .getRepairItems()
                            .size();
                }
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Excel export complete.\n\n"
                            + "Displayed RMAs exported: "
                            + displayedRmaRecords.size()
                            + "\nMachines exported: "
                            + machineCount
                            + "\n\nSaved to:\n"
                            + outputPath.toAbsolutePath(),
                    "Excel Export Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The Excel file could not be created.\n\n"
                            + exception.getMessage(),
                    "Excel Export Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String createSafeExcelFileName(String rmaNumber) {
        if (rmaNumber == null || rmaNumber.isBlank()) {
            return "RMA";
        }

        String safeName = rmaNumber.trim().replaceAll(
                "[\\/:*?\"<>|]",
                "_"
        );

        return safeName.isBlank() ? "RMA" : safeName;
    }

    private String valueOrBlank(Object value) {
        return value == null ? "" : value.toString().trim();
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

    private void backupDatabase() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Backup Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION
                || chooser.getSelectedFile() == null) {
            return;
        }

        try {
            Path backupPath = backupService.createBackup(
                    chooser.getSelectedFile().toPath()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "The database backup was created successfully.\n\n"
                            + backupPath.toAbsolutePath(),
                    "Backup Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException | SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The database backup could not be created.\n\n"
                            + exception.getMessage(),
                    "Backup Error",
                    JOptionPane.ERROR_MESSAGE
            );

            exception.printStackTrace();
        }
    }

    private void restoreDatabase() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select RMA Database Backup");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION
                || chooser.getSelectedFile() == null) {
            return;
        }

        Path selectedBackup = chooser
                .getSelectedFile()
                .toPath();

        try {
            backupService.validateBackup(selectedBackup);

        } catch (IOException | SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The selected file cannot be restored.\n\n"
                            + exception.getMessage(),
                    "Invalid Backup",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Restoring this backup will replace all current RMA data.\n\n"
                        + "A safety backup of the current database will be "
                        + "created automatically.\n\n"
                        + "Continue with the restore?",
                "Restore Database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Path safetyBackup = backupService.restoreBackup(
                    selectedBackup
            );

            clearAllFilters();
            refreshRmaTable(null);

            String safetyMessage = safetyBackup == null
                    ? "No previous database existed, so no safety backup was needed."
                    : "Safety backup created at:\n"
                      + safetyBackup.toAbsolutePath();

            JOptionPane.showMessageDialog(
                    this,
                    "The database was restored successfully.\n\n"
                            + safetyMessage,
                    "Restore Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException | SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The database could not be restored.\n\n"
                            + exception.getMessage(),
                    "Restore Error",
                    JOptionPane.ERROR_MESSAGE
            );

            exception.printStackTrace();
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

    /**
     * Formats all shipping entries for either the sent or return direction.
     */
    private String formatShippingInformation(
            RmaRecord record,
            boolean returnShipping
    ) {
        if (record == null) {
            return "-";
        }

        StringBuilder text = new StringBuilder();
        List<ShippingInfo> shippingEntries =
                record.getShippingInformation();

        if (shippingEntries != null) {
            for (ShippingInfo shippingInfo : shippingEntries) {
                if (shippingInfo == null) {
                    continue;
                }

                boolean isReturn =
                        shippingInfo.getDirection() != null
                                && shippingInfo
                                .getDirection()
                                .name()
                                .toUpperCase(Locale.ROOT)
                                .contains("RETURN");

                if (isReturn != returnShipping) {
                    continue;
                }

                String carrier =
                        shippingInfo.getCarrier() == null
                                ? ""
                                : shippingInfo.getCarrier().trim();

                String trackingNumber =
                        shippingInfo.getTrackingNumber() == null
                                ? ""
                                : shippingInfo
                                  .getTrackingNumber()
                                  .trim();

                if (carrier.isEmpty()
                        && trackingNumber.isEmpty()) {
                    continue;
                }

                if (text.length() > 0) {
                    text.append("\n");
                }

                if (!carrier.isEmpty()) {
                    text.append(carrier);
                }

                if (!carrier.isEmpty()
                        && !trackingNumber.isEmpty()) {
                    text.append(": ");
                }

                if (!trackingNumber.isEmpty()) {
                    text.append(trackingNumber);
                }
            }
        }

        /*
         * Fallback for older RMAs that still only have the legacy fields.
         */
        if (text.length() == 0) {
            String legacyValue = returnShipping
                    ? record.getReturnTrackingNumber()
                    : record.getOutgoingTrackingNumber();

            if (legacyValue != null
                    && !legacyValue.isBlank()) {
                return legacyValue.trim();
            }
        }

        return text.length() == 0
                ? "-"
                : text.toString();
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

    private static class MultilineTableCellRenderer
            extends JTextArea
            implements javax.swing.table.TableCellRenderer {

        private MultilineTableCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(
                    BorderFactory.createEmptyBorder(
                            3,
                            4,
                            3,
                            4
                    )
            );
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            setText(
                    value == null
                            ? ""
                            : value.toString()
            );

            if (isSelected) {
                setForeground(
                        table.getSelectionForeground()
                );

                setBackground(
                        table.getSelectionBackground()
                );
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            setFont(table.getFont());

            return this;
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