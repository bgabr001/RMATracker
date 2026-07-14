package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Displays the title and search/filter controls
 * at the top of MainWindow.
 *
 * The controls are created and managed by MainWindow.
 * This class is currently responsible only for layout.
 */
public class MainWindowSearchPanel extends JPanel {

    public MainWindowSearchPanel(
            JTextField searchField,
            JButton clearSearchButton,
            JComboBox<Object> statusFilterComboBox,
            JComboBox<String> countyFilterComboBox,
            JComboBox<String> machineFilterComboBox,
            JTextField dateFromField,
            JTextField dateToField
    ) {
        super(new BorderLayout(10, 10));

        setBorder(
                BorderFactory.createEmptyBorder(
                        12,
                        15,
                        0,
                        15
                )
        );

        add(
                createTitleLabel(),
                BorderLayout.WEST
        );

        add(
                createFiltersPanel(
                        searchField,
                        clearSearchButton,
                        statusFilterComboBox,
                        countyFilterComboBox,
                        machineFilterComboBox,
                        dateFromField,
                        dateToField
                ),
                BorderLayout.EAST
        );
    }

    private JLabel createTitleLabel() {
        JLabel titleLabel =
                new JLabel("RMA Tracker");

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        26
                )
        );

        return titleLabel;
    }

    private JPanel createFiltersPanel(
            JTextField searchField,
            JButton clearSearchButton,
            JComboBox<Object> statusFilterComboBox,
            JComboBox<String> countyFilterComboBox,
            JComboBox<String> machineFilterComboBox,
            JTextField dateFromField,
            JTextField dateToField
    ) {
        JPanel filtersPanel = new JPanel();

        filtersPanel.setLayout(
                new BoxLayout(
                        filtersPanel,
                        BoxLayout.Y_AXIS
                )
        );

        filtersPanel.add(
                createFirstFilterRow(
                        searchField,
                        clearSearchButton,
                        statusFilterComboBox
                )
        );

        filtersPanel.add(
                createSecondFilterRow(
                        countyFilterComboBox,
                        machineFilterComboBox,
                        dateFromField,
                        dateToField
                )
        );

        return filtersPanel;
    }

    private JPanel createFirstFilterRow(
            JTextField searchField,
            JButton clearSearchButton,
            JComboBox<Object> statusFilterComboBox
    ) {
        JPanel row =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        row.add(new JLabel("Search:"));
        row.add(searchField);
        row.add(clearSearchButton);

        row.add(
                Box.createHorizontalStrut(10)
        );

        row.add(new JLabel("Status:"));
        row.add(statusFilterComboBox);

        return row;
    }

    private JPanel createSecondFilterRow(
            JComboBox<String> countyFilterComboBox,
            JComboBox<String> machineFilterComboBox,
            JTextField dateFromField,
            JTextField dateToField
    ) {
        JPanel row =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        row.add(new JLabel("County:"));
        row.add(countyFilterComboBox);

        row.add(
                Box.createHorizontalStrut(8)
        );

        row.add(new JLabel("Machine:"));
        row.add(machineFilterComboBox);

        row.add(
                Box.createHorizontalStrut(8)
        );

        row.add(new JLabel("Sent From:"));
        row.add(dateFromField);

        row.add(new JLabel("To:"));
        row.add(dateToField);

        return row;
    }
}