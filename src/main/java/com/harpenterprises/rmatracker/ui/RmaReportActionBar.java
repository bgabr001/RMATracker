package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import java.awt.*;

/** Displays report-preview actions and delegates behavior to supplied callbacks. */
public class RmaReportActionBar extends JPanel {

    public RmaReportActionBar(
            Runnable refreshAction,
            Runnable printAction,
            Runnable savePdfAction,
            Runnable closeAction
    ) {
        super(new FlowLayout(FlowLayout.RIGHT));

        add(createButton("Refresh Preview", refreshAction));
        add(createButton("Print", printAction));
        add(createButton("Save as PDF", savePdfAction));
        add(createButton("Close", closeAction));
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }
}
