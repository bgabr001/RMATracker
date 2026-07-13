package com.harpenterprises.rmatracker;

import com.harpenterprises.rmatracker.storage.DatabaseManager;
import com.harpenterprises.rmatracker.ui.MainWindow;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseManager.initializeDatabase();

            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}