package com.harpenterprises.rmatracker;

import com.harpenterprises.rmatracker.ui.MainWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);
        });

    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception exception) {
            System.err.println(
                    "Could not apply the system appearance: "
                            + exception.getMessage()
            );
        }
    }
}