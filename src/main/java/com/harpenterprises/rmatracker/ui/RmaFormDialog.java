package com.harpenterprises.rmatracker.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class RmaFormDialog extends JDialog {

    private final JTextField rmaNumberField;
    private final JTextField dateSentField;
    private final JTextField dateReceivedField;
    private final JComboBox<String> statusComboBox;
    private final JTextField outgoingTrackingField;
    private final JTextField returnTrackingField;
    private final JTextArea notesArea;

    private final JTable repairItemsTable;
    private final DefaultTableModel repairItemsTableModel;

    private final JButton addItemButton;
    private final JButton removeItemButton;
    private final JButton saveButton;
    private final JButton cancelButton;

    public RmaFormDialog(JFrame parent) {
        super(parent, "New RMA", true);

        // Create the form fields.
        rmaNumberField = new JTextField(20);
        dateSentField = new JTextField(20);
        dateReceivedField = new JTextField(20);

        statusComboBox = new JComboBox<>(new String[]{
                "PENDING_SHIPMENT",
                "IN_PROGRESS",
                "WAITING_ON_VENDOR",
                "RETURNED",
                "COMPLETED"
        });

        outgoingTrackingField = new JTextField(20);
        returnTrackingField = new JTextField(20);

        notesArea = new JTextArea(5, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        // Create the repair-items table.
        repairItemsTableModel = new DefaultTableModel(
                new Object[]{
                        "County",
                        "Machine Type",
                        "Serial Number",
                        "Problem"
                },
                0
        );

        repairItemsTable = new JTable(repairItemsTableModel);
        repairItemsTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        repairItemsTable.setFillsViewportHeight(true);

        // Create buttons.
        addItemButton = new JButton("Add Item");
        removeItemButton = new JButton("Remove Item");
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");

        // Build the window.
        setLayout(new BorderLayout(10, 10));

        add(createFormPanel(), BorderLayout.NORTH);
        add(createRepairItemsPanel(), BorderLayout.CENTER);
        add(createBottomButtonPanel(), BorderLayout.SOUTH);

        // Connect button actions.
        addItemButton.addActionListener(e -> addRepairItem());
        removeItemButton.addActionListener(e -> removeRepairItem());
        saveButton.addActionListener(e -> saveRma());
        cancelButton.addActionListener(e -> dispose());

        getRootPane().setDefaultButton(saveButton);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(750, 650));
        setSize(800, 700);
        setLocationRelativeTo(parent);
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());

        formPanel.setBorder(
                BorderFactory.createTitledBorder("RMA Information")
        );

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.insets = new Insets(5, 8, 5, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        addFormRow(
                formPanel,
                constraints,
                row++,
                "RMA Number:",
                rmaNumberField
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Date Sent:",
                dateSentField
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Date Received:",
                dateReceivedField
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Status:",
                statusComboBox
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Outgoing Tracking:",
                outgoingTrackingField
        );

        addFormRow(
                formPanel,
                constraints,
                row++,
                "Return Tracking:",
                returnTrackingField
        );

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.NONE;

        formPanel.add(new JLabel("Notes:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.BOTH;

        JScrollPane notesScrollPane = new JScrollPane(notesArea);
        notesScrollPane.setPreferredSize(new Dimension(400, 100));

        formPanel.add(notesScrollPane, constraints);

        return formPanel;
    }

    private void addFormRow(
            JPanel panel,
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

        panel.add(new JLabel(labelText), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        panel.add(component, constraints);
    }

    private JPanel createRepairItemsPanel() {
        JPanel repairItemsPanel = new JPanel(new BorderLayout(5, 5));

        repairItemsPanel.setBorder(
                BorderFactory.createTitledBorder("Repair Items")
        );

        JScrollPane tableScrollPane =
                new JScrollPane(repairItemsTable);

        repairItemsPanel.add(
                tableScrollPane,
                BorderLayout.CENTER
        );

        JPanel itemButtonPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        itemButtonPanel.add(addItemButton);
        itemButtonPanel.add(removeItemButton);

        repairItemsPanel.add(
                itemButtonPanel,
                BorderLayout.SOUTH
        );

        return repairItemsPanel;
    }

    private JPanel createBottomButtonPanel() {
        JPanel buttonPanel =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 10, 10, 10)
        );

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }

    private void addRepairItem() {
        repairItemsTableModel.addRow(
                new Object[]{
                        "",
                        "",
                        "",
                        ""
                }
        );

        int newRow =
                repairItemsTableModel.getRowCount() - 1;

        repairItemsTable.setRowSelectionInterval(
                newRow,
                newRow
        );

        repairItemsTable.editCellAt(newRow, 0);
        repairItemsTable.requestFocusInWindow();
    }

    private void removeRepairItem() {
        int selectedRow =
                repairItemsTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a repair item to remove.",
                    "No Item Selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        repairItemsTableModel.removeRow(selectedRow);
    }

    private void saveRma() {
        String rmaNumber =
                rmaNumberField.getText().trim();

        if (rmaNumber.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The RMA number is required.",
                    "Missing RMA Number",
                    JOptionPane.WARNING_MESSAGE
            );

            rmaNumberField.requestFocusInWindow();
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "The form is working.\n"
                        + "Saving the RMA will be added in Step 6.",
                "RMA Form",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}