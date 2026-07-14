package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Builds the menu bar displayed by MainWindow.
 *
 * This class only creates menu components and connects
 * them to actions supplied by MainWindow.
 */
public class MainWindowMenuBar extends JMenuBar {

    public MainWindowMenuBar(
            Runnable backupAction,
            Runnable restoreAction,
            Runnable exitAction,
            Runnable viewSelectedReportAction,
            Runnable exportSelectedExcelAction,
            Runnable exportDisplayedExcelAction,
            Runnable exportSelectedPdfAction,
            Runnable exportDisplayedPdfAction
    ) {
        add(createFileMenu(
                backupAction,
                restoreAction,
                exitAction
        ));

        add(createReportsMenu(
                viewSelectedReportAction,
                exportSelectedExcelAction,
                exportDisplayedExcelAction,
                exportSelectedPdfAction,
                exportDisplayedPdfAction
        ));
    }

    private JMenu createFileMenu(
            Runnable backupAction,
            Runnable restoreAction,
            Runnable exitAction
    ) {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem backupItem =
                new JMenuItem("Backup Database...");

        backupItem.setMnemonic(KeyEvent.VK_B);

        backupItem.setAccelerator(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_B,
                        Toolkit.getDefaultToolkit()
                                .getMenuShortcutKeyMaskEx()
                )
        );

        backupItem.addActionListener(
                event -> backupAction.run()
        );

        JMenuItem restoreItem =
                new JMenuItem("Restore Database...");

        restoreItem.setMnemonic(KeyEvent.VK_R);

        restoreItem.addActionListener(
                event -> restoreAction.run()
        );

        JMenuItem exitItem =
                new JMenuItem("Exit");

        exitItem.addActionListener(
                event -> exitAction.run()
        );

        fileMenu.add(backupItem);
        fileMenu.add(restoreItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        return fileMenu;
    }

    private JMenu createReportsMenu(
            Runnable viewSelectedReportAction,
            Runnable exportSelectedExcelAction,
            Runnable exportDisplayedExcelAction,
            Runnable exportSelectedPdfAction,
            Runnable exportDisplayedPdfAction
    ) {
        JMenu reportsMenu = new JMenu("Reports");
        reportsMenu.setMnemonic(KeyEvent.VK_P);

        JMenuItem viewSelectedReportItem =
                new JMenuItem(
                        "View Selected RMA Report"
                );

        viewSelectedReportItem.addActionListener(
                event ->
                        viewSelectedReportAction.run()
        );

        JMenuItem exportSelectedExcelItem =
                new JMenuItem(
                        "Export Selected RMA to Excel..."
                );

        exportSelectedExcelItem.addActionListener(
                event -> exportSelectedExcelAction.run()
        );

        JMenuItem exportDisplayedExcelItem =
                new JMenuItem(
                        "Export Displayed RMAs to Excel..."
                );

        exportDisplayedExcelItem.addActionListener(
                event -> exportDisplayedExcelAction.run()
        );

        reportsMenu.add(viewSelectedReportItem);
        reportsMenu.addSeparator();
        JMenuItem exportSelectedPdfItem =
                new JMenuItem("Export Selected RMA to PDF...");
        exportSelectedPdfItem.addActionListener(
                event -> exportSelectedPdfAction.run()
        );

        JMenuItem exportDisplayedPdfItem =
                new JMenuItem("Export Displayed RMAs to PDF...");
        exportDisplayedPdfItem.addActionListener(
                event -> exportDisplayedPdfAction.run()
        );

        reportsMenu.add(exportSelectedExcelItem);
        reportsMenu.add(exportDisplayedExcelItem);
        reportsMenu.addSeparator();
        reportsMenu.add(exportSelectedPdfItem);
        reportsMenu.add(exportDisplayedPdfItem);

        return reportsMenu;
    }
}