package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Displays the main action buttons and RMA result count
 * at the bottom of MainWindow.
 *
 * MainWindow still creates the buttons and controls
 * what happens when they are clicked.
 */
public class MainWindowActionBar extends JPanel {

    public MainWindowActionBar(
            JButton newRmaButton,
            JButton openRmaButton,
            JButton viewReportButton,
            JButton statusHistoryButton,
            JButton deleteRmaButton,
            JButton refreshButton,
            JButton exitButton,
            JLabel resultLabel
    ) {
        super(new BorderLayout());

        setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        15,
                        10,
                        15
                )
        );

        add(
                createButtonPanel(
                        newRmaButton,
                        openRmaButton,
                        viewReportButton,
                        statusHistoryButton,
                        deleteRmaButton,
                        refreshButton,
                        exitButton
                ),
                BorderLayout.WEST
        );

        add(
                resultLabel,
                BorderLayout.EAST
        );
    }

    private JPanel createButtonPanel(
            JButton newRmaButton,
            JButton openRmaButton,
            JButton viewReportButton,
            JButton statusHistoryButton,
            JButton deleteRmaButton,
            JButton refreshButton,
            JButton exitButton
    ) {
        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        buttonPanel.add(newRmaButton);
        buttonPanel.add(openRmaButton);
        buttonPanel.add(viewReportButton);
        buttonPanel.add(statusHistoryButton);
        buttonPanel.add(deleteRmaButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(exitButton);

        return buttonPanel;
    }
}