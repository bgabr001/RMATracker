package com.harpenterprises.rmatracker;

import com.harpenterprises.rmatracker.storage.DatabaseManager;
import com.harpenterprises.rmatracker.ui.MainWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {

        System.setProperty("apple.awt.application.name", "RMA Tracker");

        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            exception.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage(),
                    "RMA Tracker Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
        });


        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );

                DatabaseManager.initializeDatabase();

                MainWindow window = new MainWindow();
                window.setLocationRelativeTo(null);
                window.setVisible(true);

                System.out.println("Main window opened successfully.");

            } catch (Exception exception) {
                exception.printStackTrace();

                JOptionPane.showMessageDialog(
                        null,
                        "RMA Tracker could not start.\n\n"
                                + exception.getClass().getSimpleName()
                                + ": "
                                + exception.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}