package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.StatusHistory;
import com.harpenterprises.rmatracker.storage.StatusHistoryRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StatusHistoryDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private final String rmaNumber;
    private final StatusHistoryRepository repository;
    private final DefaultTableModel tableModel;
    private final JTable historyTable;
    private final JButton deleteButton;
    private final List<StatusHistory> displayedHistory =
            new ArrayList<>();

    public StatusHistoryDialog(
            Window owner,
            String rmaNumber
    ) {
        super(
                owner,
                "Status History - " + rmaNumber,
                ModalityType.APPLICATION_MODAL
        );

        this.rmaNumber = rmaNumber;
        this.repository = new StatusHistoryRepository();

        tableModel = new DefaultTableModel(
                new Object[]{
                        "Changed At",
                        "Previous Status",
                        "New Status"
                },
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

        historyTable = new JTable(tableModel);
        historyTable.setRowHeight(25);
        historyTable.setFillsViewportHeight(true);
        historyTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        historyTable.getTableHeader()
                .setReorderingAllowed(false);

        historyTable.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(180);

        historyTable.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(180);

        historyTable.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(180);

        JLabel headingLabel = new JLabel(
                "Status changes for RMA " + rmaNumber
        );

        headingLabel.setFont(
                headingLabel.getFont()
                        .deriveFont(Font.BOLD, 18f)
        );

        headingLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        12,
                        12,
                        8,
                        12
                )
        );

        deleteButton = new JButton("Delete Selected");
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(
                event -> deleteSelectedHistory()
        );

        historyTable.getSelectionModel()
                .addListSelectionListener(event -> {
                    if (!event.getValueIsAdjusting()) {
                        deleteButton.setEnabled(
                                historyTable.getSelectedRow() >= 0
                        );
                    }
                });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(
                event -> dispose()
        );

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        buttonPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        10,
                        10,
                        10
                )
        );

        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        setLayout(new BorderLayout(10, 10));
        add(headingLabel, BorderLayout.NORTH);
        add(
                new JScrollPane(historyTable),
                BorderLayout.CENTER
        );
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(closeButton);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(650, 400);
        setMinimumSize(new Dimension(550, 300));
        setLocationRelativeTo(owner);

        loadHistory(true);
    }

    private void loadHistory(boolean showEmptyMessage) {
        tableModel.setRowCount(0);
        displayedHistory.clear();
        deleteButton.setEnabled(false);

        try {
            List<StatusHistory> historyEntries =
                    repository.findByRmaNumber(rmaNumber);

            displayedHistory.addAll(historyEntries);

            for (StatusHistory history : historyEntries) {
                tableModel.addRow(
                        new Object[]{
                                history.getChangedAt()
                                        .format(DATE_FORMATTER),
                                history.getOldStatus() == null
                                        ? "Initial Status"
                                        : history.getOldStatus(),
                                history.getNewStatus()
                        }
                );
            }

            if (showEmptyMessage && historyEntries.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No status history was found for RMA "
                                + rmaNumber + ".",
                        "No Status History",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The status history could not be loaded.\n\n"
                            + exception.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );

            exception.printStackTrace();
        }
    }

    private void deleteSelectedHistory() {
        int viewRow = historyTable.getSelectedRow();

        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a status-history entry first.",
                    "No Entry Selected",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int modelRow = historyTable.convertRowIndexToModel(viewRow);

        if (modelRow < 0 || modelRow >= displayedHistory.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The selected history entry could not be identified.",
                    "Selection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        StatusHistory selectedHistory =
                displayedHistory.get(modelRow);

        String changedAt = selectedHistory.getChangedAt()
                .format(DATE_FORMATTER);

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete this status-history entry?\n\n"
                        + "Changed At: " + changedAt + "\n"
                        + "Previous Status: "
                        + (selectedHistory.getOldStatus() == null
                        ? "Initial Status"
                        : selectedHistory.getOldStatus())
                        + "\nNew Status: "
                        + selectedHistory.getNewStatus()
                        + "\n\nThis action cannot be undone.",
                "Delete Status History",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean deleted = repository.deleteById(
                    selectedHistory.getId()
            );

            if (!deleted) {
                JOptionPane.showMessageDialog(
                        this,
                        "The history entry was not found. It may have "
                                + "already been deleted.",
                        "Entry Not Found",
                        JOptionPane.WARNING_MESSAGE
                );
            }

            loadHistory(false);

        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The status-history entry could not be deleted.\n\n"
                            + exception.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );

            exception.printStackTrace();
        }
    }
}
