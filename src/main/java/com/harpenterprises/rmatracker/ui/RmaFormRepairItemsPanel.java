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
            JButton importExcelButton
    ) {
        super(new BorderLayout(5, 5));

        setBorder(
                BorderFactory.createTitledBorder(
                        "Repair Items"
                )
        );

        add(
                new JScrollPane(repairItemsTable),
                BorderLayout.CENTER
        );

        add(
                createButtonPanel(
                        addItemButton,
                        removeItemButton,
                        importExcelButton
                ),
                BorderLayout.SOUTH
        );
    }

    private JPanel createButtonPanel(
            JButton addItemButton,
            JButton removeItemButton,
            JButton importExcelButton
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

        return buttonPanel;
    }
}
