package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Displays the repair-items table and its add/remove controls.
 */
public class RmaFormRepairItemsPanel extends JPanel {

    public RmaFormRepairItemsPanel(
            JTable repairItemsTable,
            JButton addItemButton,
            JButton removeItemButton,
            JButton importExcelButton,
            JButton selectAllMachinesButton,
            JButton deselectAllMachinesButton,
            JLabel machineSelectionCountLabel,
            JTextField machineSearchField,
            JComboBox<String> machineTypeFilter,
            JButton clearMachineFiltersButton
    ) {
        super(new BorderLayout(5, 5));

        setBorder(
                BorderFactory.createTitledBorder(
                        "Repair Items"
                )
        );

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Search machines:"));
        filterPanel.add(machineSearchField);
        filterPanel.add(new JLabel("Type:"));
        filterPanel.add(machineTypeFilter);
        filterPanel.add(clearMachineFiltersButton);
        add(filterPanel, BorderLayout.NORTH);

        add(
                new JScrollPane(repairItemsTable),
                BorderLayout.CENTER
        );

        add(
                createButtonPanel(
                        addItemButton,
                        removeItemButton,
                        importExcelButton,
                        selectAllMachinesButton,
                        deselectAllMachinesButton,
                        machineSelectionCountLabel
                ),
                BorderLayout.SOUTH
        );
    }

    private JPanel createButtonPanel(
            JButton addItemButton,
            JButton removeItemButton,
            JButton importExcelButton,
            JButton selectAllMachinesButton,
            JButton deselectAllMachinesButton,
            JLabel machineSelectionCountLabel
    ) {
        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        buttonPanel.add(addItemButton);
        buttonPanel.add(removeItemButton);
        buttonPanel.add(importExcelButton);
        buttonPanel.add(selectAllMachinesButton);
        buttonPanel.add(deselectAllMachinesButton);
        buttonPanel.add(machineSelectionCountLabel);

        return buttonPanel;
    }
}
