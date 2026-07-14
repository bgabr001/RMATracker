package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Displays the shipping table and its add/remove controls.
 */
public class RmaFormShippingPanel extends JPanel {

    public RmaFormShippingPanel(
            JTable shippingTable,
            JButton addShippingButton,
            JButton removeShippingButton
    ) {
        super(new BorderLayout(5, 5));

        setBorder(
                BorderFactory.createTitledBorder(
                        "Shipping Information"
                )
        );

        JScrollPane scrollPane =
                new JScrollPane(shippingTable);

        scrollPane.setPreferredSize(
                new Dimension(850, 115)
        );

        add(scrollPane, BorderLayout.CENTER);
        add(
                createButtonPanel(
                        addShippingButton,
                        removeShippingButton
                ),
                BorderLayout.SOUTH
        );
    }

    private JPanel createButtonPanel(
            JButton addShippingButton,
            JButton removeShippingButton
    ) {
        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        buttonPanel.add(addShippingButton);
        buttonPanel.add(
                new JLabel("Add another shipment")
        );
        buttonPanel.add(removeShippingButton);

        return buttonPanel;
    }
}
