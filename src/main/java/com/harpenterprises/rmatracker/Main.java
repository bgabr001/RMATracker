package com.harpenterprises.rmatracker;

import com.harpenterprises.rmatracker.storage.DatabaseManager;
import com.harpenterprises.rmatracker.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {

        // Create the database file and tables before opening the GUI.
        DatabaseManager.initializeDatabase();


        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception exception) {
                System.err.println(
                        "Could not load the system look and feel."
                );
            }

            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);
        });
    }
}