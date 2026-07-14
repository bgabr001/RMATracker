package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Displays information about the currently selected RMA,
 * including shipping, notes, and repair items.
 *
 * MainWindow remains responsible for placing data into
 * the components.
 */
public class MainWindowDetailsPanel extends JPanel {

    public MainWindowDetailsPanel(
            JLabel detailMessageLabel,
            JLabel selectedRmaNumberValue,
            JLabel selectedDateSentValue,
            JLabel selectedDateReceivedValue,
            JLabel selectedStatusValue,
            JTextArea selectedSentShippingValue,
            JTextArea selectedReturnShippingValue,
            JTextArea selectedNotesArea,
            JTable repairItemsTable
    ) {
        super(new BorderLayout(10, 10));

        setBorder(
                BorderFactory.createTitledBorder(
                        "Selected RMA Details"
                )
        );

        configureMessageLabel(detailMessageLabel);

        add(
                detailMessageLabel,
                BorderLayout.NORTH
        );

        add(
                createDetailsSplitPane(
                        selectedRmaNumberValue,
                        selectedDateSentValue,
                        selectedDateReceivedValue,
                        selectedStatusValue,
                        selectedSentShippingValue,
                        selectedReturnShippingValue,
                        selectedNotesArea,
                        repairItemsTable
                ),
                BorderLayout.CENTER
        );
    }

    private void configureMessageLabel(
            JLabel detailMessageLabel
    ) {
        detailMessageLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        8,
                        5,
                        8
                )
        );
    }

    private JSplitPane createDetailsSplitPane(
            JLabel selectedRmaNumberValue,
            JLabel selectedDateSentValue,
            JLabel selectedDateReceivedValue,
            JLabel selectedStatusValue,
            JTextArea selectedSentShippingValue,
            JTextArea selectedReturnShippingValue,
            JTextArea selectedNotesArea,
            JTable repairItemsTable
    ) {
        JSplitPane splitPane =
                new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        createRmaInformationPanel(
                                selectedRmaNumberValue,
                                selectedDateSentValue,
                                selectedDateReceivedValue,
                                selectedStatusValue,
                                selectedSentShippingValue,
                                selectedReturnShippingValue,
                                selectedNotesArea
                        ),
                        createRepairItemsPanel(
                                repairItemsTable
                        )
                );

        splitPane.setResizeWeight(0.32);
        splitPane.setDividerLocation(390);

        return splitPane;
    }

    private JPanel createRepairItemsPanel(
            JTable repairItemsTable
    ) {
        JPanel panel =
                new JPanel(new BorderLayout());

        panel.setBorder(
                BorderFactory.createTitledBorder(
                        "Repair Items"
                )
        );

        panel.add(
                new JScrollPane(repairItemsTable),
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel createRmaInformationPanel(
            JLabel selectedRmaNumberValue,
            JLabel selectedDateSentValue,
            JLabel selectedDateReceivedValue,
            JLabel selectedStatusValue,
            JTextArea selectedSentShippingValue,
            JTextArea selectedReturnShippingValue,
            JTextArea selectedNotesArea
    ) {
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
                "Sent Shipping:",
                createShippingScrollPane(
                        selectedSentShippingValue
                )
        );

        addDetailRow(
                panel,
                constraints,
                row++,
                "Return Shipping:",
                createShippingScrollPane(
                        selectedReturnShippingValue
                )
        );

        addNotesRow(
                panel,
                constraints,
                row,
                selectedNotesArea
        );

        return panel;
    }

    private void addDetailRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String labelText,
            JComponent valueComponent
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill =
                GridBagConstraints.NONE;

        panel.add(
                createBoldLabel(labelText),
                constraints
        );

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        panel.add(
                valueComponent,
                constraints
        );
    }

    private void addNotesRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            JTextArea selectedNotesArea
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill =
                GridBagConstraints.NONE;

        panel.add(
                createBoldLabel("Notes:"),
                constraints
        );

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill =
                GridBagConstraints.BOTH;

        panel.add(
                new JScrollPane(selectedNotesArea),
                constraints
        );
    }

    private JLabel createBoldLabel(
            String text
    ) {
        JLabel label =
                new JLabel(text);

        label.setFont(
                label.getFont()
                        .deriveFont(Font.BOLD)
        );

        return label;
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
}