package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.Status;

import javax.swing.*;
import java.awt.*;

/**
 * Lays out the RMA-level fields used by RmaFormDialog.
 * The dialog owns the components and all save/validation behavior.
 */
public class RmaFormInformationPanel extends JPanel {

    public RmaFormInformationPanel(
            JTextField rmaNumberField,
            JTextField dateSentField,
            JTextField dateReceivedField,
            JComboBox<Status> statusComboBox,
            JTextArea notesArea
    ) {
        super(new GridBagLayout());

        setBorder(
                BorderFactory.createTitledBorder(
                        "RMA Information"
                )
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.insets = new Insets(5, 8, 5, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        addFormRow(
                constraints,
                row++,
                "RMA Number:",
                rmaNumberField
        );

        addFormRow(
                constraints,
                row++,
                "Date Sent (YYYY-MM-DD):",
                dateSentField
        );

        addFormRow(
                constraints,
                row++,
                "Date Received (YYYY-MM-DD):",
                dateReceivedField
        );

        addFormRow(
                constraints,
                row++,
                "Status:",
                statusComboBox
        );

        addNotesRow(
                constraints,
                row,
                notesArea
        );
    }

    private void addFormRow(
            GridBagConstraints constraints,
            int row,
            String labelText,
            JComponent component
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;

        add(new JLabel(labelText), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        add(component, constraints);
    }

    private void addNotesRow(
            GridBagConstraints constraints,
            int row,
            JTextArea notesArea
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.NONE;

        add(new JLabel("Notes:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.BOTH;

        JScrollPane notesScrollPane =
                new JScrollPane(notesArea);

        notesScrollPane.setPreferredSize(
                new Dimension(500, 100)
        );

        add(notesScrollPane, constraints);
    }
}
