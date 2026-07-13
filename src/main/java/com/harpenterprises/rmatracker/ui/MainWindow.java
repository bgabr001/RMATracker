package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.storage.RmaRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MainWindow extends JFrame {

    private final JTextField searchField;
    private final JTable rmaTable;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> tableSorter;

    private final JButton newRmaButton;
    private final JButton openRmaButton;
    private final JButton deleteRmaButton;
    private final JButton refreshButton;
    private final JButton exitButton;

    private final RmaRepository repository;

    public MainWindow() {
        repository = new RmaRepository();

        setTitle("RMA Repair Tracker");
        setSize(1200, 650);
        setMinimumSize(new Dimension(900, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        searchField = new JTextField(25);

        newRmaButton = new JButton("New RMA");
        openRmaButton = new JButton("Open RMA");
        deleteRmaButton = new JButton("Delete RMA");
        refreshButton = new JButton("Refresh");
        exitButton = new JButton("Exit");

        String[] columnNames = {
                "RMA Number",
                "Date Sent (YYYY-MM-DD)",
                "Date Received (YYYY-MM-DD)",
                "Status",
                "Outgoing Tracking",
                "Return Tracking",
                "Repair Items",
                "Notes"
        };

        tableModel = new DefaultTableModel(
                columnNames,
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

        rmaTable = new JTable(tableModel);
        rmaTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        rmaTable.setRowHeight(25);
        rmaTable
                .getTableHeader()
                .setReorderingAllowed(false);

        rmaTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF
        );

        configureColumnWidths();

        tableSorter =
                new TableRowSorter<>(tableModel);

        rmaTable.setRowSorter(tableSorter);

        setLayout(new BorderLayout(10, 10));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        addButtonActions();
        addSearchFunctionality();
        addDoubleClickAction();

        refreshRmaTable();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel =
                new JPanel(new BorderLayout(10, 10));

        headerPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
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

        JPanel searchPanel =
                new JPanel(
                        new FlowLayout(FlowLayout.RIGHT)
                );

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);

        headerPanel.add(
                titleLabel,
                BorderLayout.WEST
        );

        headerPanel.add(
                searchPanel,
                BorderLayout.EAST
        );

        return headerPanel;
    }

    private JScrollPane createTablePanel() {
        JScrollPane scrollPane =
                new JScrollPane(rmaTable);

        scrollPane.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(
                                10,
                                15,
                                10,
                                15
                        ),
                        BorderFactory.createLineBorder(
                                Color.GRAY
                        )
                )
        );

        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                10
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

        buttonPanel.add(newRmaButton);
        buttonPanel.add(openRmaButton);
        buttonPanel.add(deleteRmaButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(
                Box.createHorizontalStrut(20)
        );
        buttonPanel.add(exitButton);

        return buttonPanel;
    }

    private void configureColumnWidths() {
        rmaTable
                .getColumnModel()
                .getColumn(0)
                .setPreferredWidth(120);

        rmaTable
                .getColumnModel()
                .getColumn(1)
                .setPreferredWidth(150);

        rmaTable
                .getColumnModel()
                .getColumn(2)
                .setPreferredWidth(170);

        rmaTable
                .getColumnModel()
                .getColumn(3)
                .setPreferredWidth(150);

        rmaTable
                .getColumnModel()
                .getColumn(4)
                .setPreferredWidth(180);

        rmaTable
                .getColumnModel()
                .getColumn(5)
                .setPreferredWidth(180);

        rmaTable
                .getColumnModel()
                .getColumn(6)
                .setPreferredWidth(100);

        rmaTable
                .getColumnModel()
                .getColumn(7)
                .setPreferredWidth(300);
    }

    private void addButtonActions() {
        newRmaButton.addActionListener(
                event -> openNewRmaWindow()
        );

        openRmaButton.addActionListener(
                event -> openSelectedRma()
        );

        deleteRmaButton.addActionListener(
                event -> deleteSelectedRma()
        );

        refreshButton.addActionListener(
                event -> refreshRmaTable()
        );

        exitButton.addActionListener(
                event -> exitApplication()
        );
    }

    private void addSearchFunctionality() {
        searchField
                .getDocument()
                .addDocumentListener(
                        new javax.swing.event.DocumentListener() {

                            @Override
                            public void insertUpdate(
                                    javax.swing.event.DocumentEvent event
                            ) {
                                filterTable();
                            }

                            @Override
                            public void removeUpdate(
                                    javax.swing.event.DocumentEvent event
                            ) {
                                filterTable();
                            }

                            @Override
                            public void changedUpdate(
                                    javax.swing.event.DocumentEvent event
                            ) {
                                filterTable();
                            }
                        }
                );
    }

    private void filterTable() {
        String searchText =
                searchField.getText().trim();

        if (searchText.isEmpty()) {
            tableSorter.setRowFilter(null);
            return;
        }

        tableSorter.setRowFilter(
                RowFilter.regexFilter(
                        "(?i)"
                                + Pattern.quote(searchText)
                )
        );
    }

    private void addDoubleClickAction() {
        rmaTable.addMouseListener(
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            java.awt.event.MouseEvent event
                    ) {
                        if (event.getClickCount() == 2) {
                            openSelectedRma();
                        }
                    }
                }
        );
    }

    private void openNewRmaWindow() {
        RmaFormDialog dialog =
                new RmaFormDialog(this);

        dialog.setVisible(true);

        if (dialog.isSaved()) {
            refreshRmaTable();
        }
    }

    private void openSelectedRma() {
        String rmaNumber =
                getSelectedRmaNumber();

        if (rmaNumber == null) {
            return;
        }

        try {
            Optional<RmaRecord> result =
                    repository.findByRmaNumber(
                            rmaNumber
                    );

            if (result.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "The selected RMA could not be found.",
                        "RMA Not Found",
                        JOptionPane.WARNING_MESSAGE
                );

                refreshRmaTable();
                return;
            }

            RmaFormDialog dialog =
                    new RmaFormDialog(
                            this,
                            result.get()
                    );

            dialog.setVisible(true);

            if (dialog.isSaved()) {
                refreshRmaTable();
            }

        } catch (SQLException exception) {
            showDatabaseError(
                    "The RMA could not be opened.",
                    exception
            );
        }
    }

    private void deleteSelectedRma() {
        String rmaNumber =
                getSelectedRmaNumber();

        if (rmaNumber == null) {
            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to delete RMA "
                                + rmaNumber
                                + "?\n\n"
                                + "Its repair items will also be deleted.",
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

            if (deleted) {
                refreshRmaTable();

                JOptionPane.showMessageDialog(
                        this,
                        "RMA "
                                + rmaNumber
                                + " was deleted.",
                        "RMA Deleted",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "The selected RMA could not be found.",
                        "RMA Not Found",
                        JOptionPane.WARNING_MESSAGE
                );
            }

        } catch (SQLException exception) {
            showDatabaseError(
                    "The RMA could not be deleted.",
                    exception
            );
        }
    }

    private String getSelectedRmaNumber() {
        int selectedViewRow =
                rmaTable.getSelectedRow();

        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an RMA.",
                    "No RMA Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        int selectedModelRow =
                rmaTable.convertRowIndexToModel(
                        selectedViewRow
                );

        Object value =
                tableModel.getValueAt(
                        selectedModelRow,
                        0
                );

        return value == null
                ? null
                : value.toString();
    }

    private void refreshRmaTable() {
        try {
            List<RmaRecord> records =
                    repository.findAll();

            tableModel.setRowCount(0);

            for (RmaRecord record : records) {
                tableModel.addRow(
                        new Object[]{
                                record.getRmaNumber(),
                                formatValue(
                                        record.getDateSent()
                                ),
                                formatValue(
                                        record.getDateReceived()
                                ),
                                record.getStatus(),
                                emptyIfNull(
                                        record
                                                .getOutgoingTrackingNumber()
                                ),
                                emptyIfNull(
                                        record
                                                .getReturnTrackingNumber()
                                ),
                                record
                                        .getRepairItems()
                                        .size(),
                                emptyIfNull(
                                        record.getNotes()
                                )
                        }
                );
            }

        } catch (SQLException exception) {
            showDatabaseError(
                    "The RMA table could not be loaded.",
                    exception
            );
        }
    }

    private Object formatValue(Object value) {
        return value == null ? "" : value;
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private void showDatabaseError(
            String message,
            SQLException exception
    ) {
        JOptionPane.showMessageDialog(
                this,
                message
                        + "\n\n"
                        + exception.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );

        exception.printStackTrace();
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
}