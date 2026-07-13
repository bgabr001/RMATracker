package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Displays a complete preview of one RMA and every machine attached to it.
 *
 * Version 2.0 - Reports
 * Step 1: Report preview window
 */
public class RmaReportPreviewWindow extends JDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RmaRecord rmaRecord;
    private final JEditorPane reportPane;

    public RmaReportPreviewWindow(
            Window owner,
            RmaRecord rmaRecord
    ) {
        super(
                owner,
                "RMA Report Preview",
                ModalityType.APPLICATION_MODAL
        );

        if (rmaRecord == null) {
            throw new IllegalArgumentException(
                    "RMA record cannot be null."
            );
        }

        this.rmaRecord = rmaRecord;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 700);
        setMinimumSize(new Dimension(900, 500));
        setLocationRelativeTo(owner);

        JPanel mainPanel =
                new JPanel(new BorderLayout(10, 10));

        mainPanel.setBorder(
                new EmptyBorder(12, 12, 12, 12)
        );

        JLabel titleLabel = new JLabel(
                "RMA Report: "
                        + safeText(rmaRecord.getRmaNumber())
        );

        titleLabel.setFont(
                titleLabel.getFont()
                        .deriveFont(Font.BOLD, 22f)
        );

        mainPanel.add(titleLabel, BorderLayout.NORTH);

        reportPane = new JEditorPane();
        reportPane.setEditable(false);
        reportPane.setContentType("text/html");
        reportPane.setEditorKit(new HTMLEditorKit());
        reportPane.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                Boolean.TRUE
        );
        reportPane.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        reportPane.setText(buildReportHtml());
        reportPane.setCaretPosition(0);

        JScrollPane scrollPane =
                new JScrollPane(reportPane);

        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        new Color(190, 190, 190)
                )
        );

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(
                createButtonPanel(),
                BorderLayout.SOUTH
        );

        setContentPane(mainPanel);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        JButton refreshButton =
                new JButton("Refresh Preview");

        JButton printButton =
                new JButton("Print");

        JButton savePdfButton =
                new JButton("Save as PDF");

        JButton closeButton =
                new JButton("Close");

        refreshButton.addActionListener(
                event -> refreshReport()
        );

        printButton.addActionListener(
                event -> JOptionPane.showMessageDialog(
                        this,
                        "Printing will be added in Version 2, Step 3.",
                        "Print RMA Report",
                        JOptionPane.INFORMATION_MESSAGE
                )
        );

        savePdfButton.addActionListener(
                event -> JOptionPane.showMessageDialog(
                        this,
                        "PDF export will be added in Version 2, Step 5.",
                        "Export RMA Report",
                        JOptionPane.INFORMATION_MESSAGE
                )
        );

        closeButton.addActionListener(
                event -> dispose()
        );

        buttonPanel.add(refreshButton);
        buttonPanel.add(printButton);
        buttonPanel.add(savePdfButton);
        buttonPanel.add(closeButton);

        return buttonPanel;
    }

    private void refreshReport() {
        reportPane.setText(buildReportHtml());
        reportPane.setCaretPosition(0);
    }

    private String buildReportHtml() {
        StringBuilder html = new StringBuilder();

        html.append("""
                <html>
                <head>
                    <style>
                        body {
                            font-family: sans-serif;
                            margin: 18px;
                            color: #222222;
                        }

                        h1 {
                            color: #17365d;
                            margin-bottom: 4px;
                        }

                        h2 {
                            color: #17365d;
                            border-bottom: 2px solid #7f9db9;
                            padding-bottom: 4px;
                            margin-top: 24px;
                        }

                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 10px;
                        }

                        th {
                            background-color: #d9eaf7;
                            text-align: left;
                            padding: 8px;
                            border: 1px solid #999999;
                        }

                        td {
                            padding: 8px;
                            border: 1px solid #999999;
                            vertical-align: top;
                        }

                        .label {
                            font-weight: bold;
                            width: 190px;
                            background-color: #f1f1f1;
                        }


                        .empty-message {
                            padding: 14px;
                            background-color: #fff4cc;
                            border: 1px solid #d6b656;
                        }

                        .footer {
                            margin-top: 30px;
                            font-size: 10px;
                            color: #666666;
                        }
                    </style>
                </head>
                <body>
                """);

        html.append("<h1>RMA Report</h1>");

        html.append("<p>RMA Number: <strong>")
                .append(
                        htmlEscape(
                                safeText(
                                        rmaRecord.getRmaNumber()
                                )
                        )
                )
                .append("</strong></p>");

        appendRmaInformation(html);
        appendMachineInformation(html);
        appendStatusHistoryPlaceholder(html);

        html.append("<div class='footer'>")
                .append("Report preview generated on ")
                .append(
                        LocalDate.now()
                                .format(DATE_FORMATTER)
                )
                .append("</div>");

        html.append("""
                </body>
                </html>
                """);

        return html.toString();
    }

    private void appendRmaInformation(
            StringBuilder html
    ) {
        html.append("<h2>RMA Information</h2>");
        html.append("<table>");

        appendInformationRow(
                html,
                "RMA Number",
                safeText(rmaRecord.getRmaNumber())
        );

        appendInformationRow(
                html,
                "Status",
                rmaRecord.getStatus() == null
                        ? ""
                        : rmaRecord.getStatus().toString()
        );

        appendInformationRow(
                html,
                "Date Sent",
                formatDate(rmaRecord.getDateSent())
        );

        appendInformationRow(
                html,
                "Date Received",
                formatDate(rmaRecord.getDateReceived())
        );

        appendInformationRow(
                html,
                "Outgoing Tracking",
                safeText(
                        rmaRecord
                                .getOutgoingTrackingNumber()
                )
        );

        appendInformationRow(
                html,
                "Return Tracking",
                safeText(
                        rmaRecord
                                .getReturnTrackingNumber()
                )
        );

        appendInformationRow(
                html,
                "General Notes",
                formatMultilineText(
                        rmaRecord.getNotes()
                )
        );

        html.append("</table>");
    }

    private void appendMachineInformation(
            StringBuilder html
    ) {
        html.append("<h2>Machines Included - Table View</h2>");

        List<RepairItem> repairItems =
                rmaRecord.getRepairItems();

        if (repairItems == null
                || repairItems.isEmpty()) {

            html.append("""
                    <div class='empty-message'>
                        No machines are currently attached to this RMA.
                    </div>
                    """);

            return;
        }

        html.append("<p><strong>Total Machines:</strong> ")
                .append(repairItems.size())
                .append("</p>");

        html.append("<table>");
        html.append("<tr>");
        html.append("<th>County</th>");
        html.append("<th>Serial Number</th>");
        html.append("<th>Equipment Type</th>");
        html.append("<th>RMA Number</th>");
        html.append("<th>Problem Description</th>");
        html.append("<th>Repair Description</th>");
        html.append("</tr>");

        for (RepairItem repairItem : repairItems) {
            html.append("<tr>");

            appendMachineCell(
                    html,
                    repairItem.getCounty()
            );

            appendMachineCell(
                    html,
                    repairItem.getSerialNumber()
            );

            appendMachineCell(
                    html,
                    repairItem.getMachineType()
            );

            appendMachineCell(
                    html,
                    rmaRecord.getRmaNumber()
            );

            appendMachineCell(
                    html,
                    repairItem.getProblemDescription()
            );

            appendMachineCell(
                    html,
                    repairItem.getRepairDescription()
            );

            html.append("</tr>");
        }

        html.append("</table>");
    }

    private void appendMachineCell(
            StringBuilder html,
            String value
    ) {
        html.append("<td>");

        if (value == null || value.isBlank()) {
            html.append("&mdash;");
        } else {
            html.append(formatMultilineText(value));
        }

        html.append("</td>");
    }

    private void appendStatusHistoryPlaceholder(
            StringBuilder html
    ) {
        html.append("<h2>Status History</h2>");

        html.append("""
                <div class='empty-message'>
                    Status history will be connected to this report
                    during Version 2, Step 2.
                </div>
                """);
    }

    private void appendInformationRow(
            StringBuilder html,
            String label,
            String value
    ) {
        html.append("<tr>");

        html.append("<td class='label'>")
                .append(htmlEscape(label))
                .append("</td>");

        html.append("<td>")
                .append(
                        value == null || value.isBlank()
                                ? "&mdash;"
                                : value
                )
                .append("</td>");

        html.append("</tr>");
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }

        return htmlEscape(
                date.format(DATE_FORMATTER)
        );
    }

    private String formatMultilineText(
            String text
    ) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return htmlEscape(text)
                .replace("\r\n", "<br>")
                .replace("\n", "<br>")
                .replace("\r", "<br>");
    }

    private String safeText(Object value) {
        return value == null
                ? ""
                : value.toString();
    }

    private String htmlEscape(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
