package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Displays Save and Cancel actions at the bottom of RmaFormDialog.
 */
public class RmaFormActionBar extends JPanel {

    public RmaFormActionBar(
            JButton saveButton,
            JButton cancelButton
    ) {
        super(new FlowLayout(FlowLayout.RIGHT));

        setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        10,
                        10,
                        10
                )
        );

        add(saveButton);
        add(cancelButton);
    }
}
