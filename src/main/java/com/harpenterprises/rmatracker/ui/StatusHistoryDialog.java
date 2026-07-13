package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.StatusHistory;
import com.harpenterprises.rmatracker.storage.StatusHistoryRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StatusHistoryDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private final String rmaNumber;
    private final StatusHistoryRepository repository;
    private final DefaultTableModel tableModel;

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

        JTable historyTable = new JTable(tableModel);
        historyTable.setRowHeight(25);
        historyTable.setFillsViewportHeight(true);
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

        loadHistory();
    }

    private void loadHistory() {
        tableModel.setRowCount(0);

        try {
            List<StatusHistory> historyEntries =
                    repository.findByRmaNumber(rmaNumber);

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

            if (historyEntries.isEmpty()) {
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
}
