package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

import com.harpenterprises.rmatracker.ui.RmaFormDialog;

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

    public MainWindow() {
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

        String[] columnsNames = {
                "RMA Number," ,
                "Date Sent",
                "Date Recieved",
                "Status",
                "Outgoing Tracking",
                "Return Tracking",
                "Repair Items",
                "Notes"
        };

        tableModel = new DefaultTableModel(columnsNames, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };



        rmaTable = new JTable(tableModel);
        rmaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rmaTable.setRowHeight(25);
        rmaTable.getTableHeader().setReorderingAllowed(false);
        rmaTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        configureColumnWidths();

        tableSorter = new TableRowSorter<>(tableModel);
        rmaTable.setRowSorter(tableSorter);

        setLayout(new BorderLayout(10, 10));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        addButtonActions();
        addSearchFunctionality();
        addDoubleClickAction();

        loadSampleData();

    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));

        JLabel titleLabel = new JLabel("RMA Repair Tracker");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 26));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JLabel searchLabel = new JLabel("Search:");
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JScrollPane createTablePanel() {
        JScrollPane scrollPane = new JScrollPane(rmaTable);

        scrollPane.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(10, 15, 10, 15),
                        BorderFactory.createLineBorder(Color.GRAY)
                )
        );

        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        buttonPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 10, 10, 10)
        );

        buttonPanel.add(newRmaButton);
        buttonPanel.add(openRmaButton);
        buttonPanel.add(deleteRmaButton);
        buttonPanel.add(refreshButton);

        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(exitButton);

        return buttonPanel;
    }

    private void configureColumnWidths() {
        rmaTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        rmaTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        rmaTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        rmaTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        rmaTable.getColumnModel().getColumn(4).setPreferredWidth(180);
        rmaTable.getColumnModel().getColumn(5).setPreferredWidth(180);
        rmaTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        rmaTable.getColumnModel().getColumn(7).setPreferredWidth(300);
    }

    private void addButtonActions() {
        newRmaButton.addActionListener(event -> openNewRmaWindow());

        openRmaButton.addActionListener(event -> openSelectedRma());

        deleteRmaButton.addActionListener(event -> deleteSelectedRma());

        refreshButton.addActionListener(event -> refreshRmaTable());

        exitButton.addActionListener(event -> exitApplication());
    }


    private void addSearchFunctionality() {
        searchField.getDocument().addDocumentListener(
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
        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(
                    RowFilter.regexFilter(
                            "(?i)" + java.util.regex.Pattern.quote(searchText)
                    )
            );
        }
    }

    private void addDoubleClickAction() {
        rmaTable.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    openSelectedRma();
                }
            }
        });
    }

    private void openNewRmaWindow() {
        RmaFormDialog dialog = new RmaFormDialog(this);
        dialog.setVisible(true);
    }

    private void openSelectedRma() {
        int selectedViewRow = rmaTable.getSelectedRow();

        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an RMA to open.",
                    "No RMA Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int selectedModelRow =
                rmaTable.convertRowIndexToModel(selectedViewRow);

        String rmaNumber = tableModel
                .getValueAt(selectedModelRow, 0)
                .toString();

        JOptionPane.showMessageDialog(
                this,
                "Opening RMA: " + rmaNumber
                        + "\n\nThe RMA details window will be added later.",
                "Open RMA",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void deleteSelectedRma() {
        int selectedViewRow = rmaTable.getSelectedRow();

        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an RMA to delete.",
                    "No RMA Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int selectedModelRow =
                rmaTable.convertRowIndexToModel(selectedViewRow);

        String rmaNumber = tableModel
                .getValueAt(selectedModelRow, 0)
                .toString();

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete RMA "
                        + rmaNumber + "?",
                "Delete RMA",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            tableModel.removeRow(selectedModelRow);
        }

    }

    private void refreshRmaTable() {
        JOptionPane.showMessageDialog(
                this,
                "The RMA table has been refreshed.",
                "Refresh",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void exitApplication() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit?",
                "Exit RMA Tracker",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    private void loadSampleData() {
        tableModel.addRow(new Object[]{
                "RMA-2026-001",
                "2026-07-01",
                "",
                "SENT",
                "1Z123456789",
                "",
                3,
                "Waiting for repair"
        });

        tableModel.addRow(new Object[]{
                "RMA-2026-002",
                "2026-06-20",
                "2026-07-08",
                "COMPLETE",
                "1Z987654321",
                "1Z456789123",
                2,
                "All machines returned"
        });

        tableModel.addRow(new Object[]{
                "RMA-2026-003",
                "2026-07-09",
                "",
                "IN_PROGRESS",
                "1Z555666777",
                "",
                4,
                "Vendor is currently repairing machines"
        });
    }

}
