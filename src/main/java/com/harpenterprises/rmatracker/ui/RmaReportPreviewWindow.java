package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.ShippingInfo;
import com.harpenterprises.rmatracker.model.StatusHistory;
import com.harpenterprises.rmatracker.storage.StatusHistoryRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Displays and prints a complete report for one selected RMA.
 *
 * Version 2.0 - Reports
 * Step 3: Printable RMA reports
 */
public class RmaReportPreviewWindow extends JDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter HISTORY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private static final double POINTS_PER_INCH = 72.0;
    private static final double DEFAULT_MARGIN = 0.50 * POINTS_PER_INCH;
    private static final double HEADER_HEIGHT = 28.0;
    private static final double FOOTER_HEIGHT = 24.0;

    private final RmaRecord rmaRecord;
    private final JEditorPane reportPane;
    private final StatusHistoryRepository statusHistoryRepository;

    private PageFormat pageFormat;

    public RmaReportPreviewWindow(
            Window owner,
            RmaRecord rmaRecord
    ) {
        super(
                owner,
                "RMA Report",
                ModalityType.APPLICATION_MODAL
        );

        if (rmaRecord == null) {
            throw new IllegalArgumentException(
                    "RMA record cannot be null."
            );
        }

        this.rmaRecord = rmaRecord;
        this.statusHistoryRepository =
                new StatusHistoryRepository();
        this.pageFormat = createDefaultPageFormat();

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
                new JButton("Refresh Report");

        JButton printPreviewButton =
                new JButton("Print Preview...");

        JButton closeButton =
                new JButton("Close");

        refreshButton.addActionListener(
                event -> refreshReport()
        );

        printPreviewButton.addActionListener(
                event -> {
                    RmaPrintPreviewWindow previewWindow =
                            new RmaPrintPreviewWindow(
                                    this,
                                    rmaRecord
                            );

                    previewWindow.setVisible(true);
                }
        );

        closeButton.addActionListener(
                event -> dispose()
        );

        buttonPanel.add(refreshButton);
        buttonPanel.add(printPreviewButton);
        buttonPanel.add(closeButton);

        return buttonPanel;
    }

    private PageFormat createDefaultPageFormat() {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        PageFormat format = printerJob.defaultPage();

        Paper paper = format.getPaper();

        paper.setImageableArea(
                DEFAULT_MARGIN,
                DEFAULT_MARGIN,
                paper.getWidth() - (DEFAULT_MARGIN * 2),
                paper.getHeight() - (DEFAULT_MARGIN * 2)
        );

        format.setPaper(paper);
        format.setOrientation(PageFormat.PORTRAIT);

        return printerJob.validatePage(format);
    }

    private void showPageSetup() {
        try {
            PrinterJob printerJob = PrinterJob.getPrinterJob();

            PageFormat selectedFormat =
                    printerJob.pageDialog(pageFormat);

            if (selectedFormat != null) {
                pageFormat =
                        printerJob.validatePage(selectedFormat);
            }
        } catch (RuntimeException exception) {
            showPrintError(
                    "The page setup window could not be opened.",
                    exception
            );
        }
    }

    private void printReport() {
        refreshReport();

        PrinterJob printerJob = PrinterJob.getPrinterJob();

        printerJob.setJobName(
                "RMA Report - "
                        + safeText(rmaRecord.getRmaNumber())
        );

        printerJob.setPrintable(
                createReportPrintable(),
                pageFormat
        );

        try {
            if (!printerJob.printDialog()) {
                return;
            }

            printerJob.print();

            JOptionPane.showMessageDialog(
                    this,
                    "The RMA report was sent to the printer.",
                    "Print Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (PrinterException exception) {
            showPrintError(
                    "The RMA report could not be printed.",
                    exception
            );
        } catch (RuntimeException exception) {
            showPrintError(
                    "An unexpected printing error occurred.",
                    exception
            );
        }
    }

    private Printable createReportPrintable() {
        return (graphics, format, pageIndex) -> {
            Graphics2D graphics2D =
                    (Graphics2D) graphics.create();

            try {
                graphics2D.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                double printableWidth =
                        format.getImageableWidth();

                double printableBodyHeight =
                        format.getImageableHeight()
                                - HEADER_HEIGHT
                                - FOOTER_HEIGHT;

                if (printableWidth <= 0
                        || printableBodyHeight <= 0) {

                    return Printable.NO_SUCH_PAGE;
                }

                int componentWidth =
                        Math.max(
                                1,
                                reportPane.getPreferredSize().width
                        );

                double scale =
                        printableWidth / componentWidth;

                /*
                 * Do not enlarge the report beyond its normal size.
                 */
                scale = Math.min(scale, 1.0);

                int targetWidth =
                        Math.max(
                                1,
                                (int) Math.floor(
                                        printableWidth / scale
                                )
                        );

                reportPane.setSize(
                        targetWidth,
                        Integer.MAX_VALUE
                );

                Dimension preferredSize =
                        reportPane.getPreferredSize();

                reportPane.setSize(
                        targetWidth,
                        preferredSize.height
                );

                double pageBodyHeightInComponentUnits =
                        printableBodyHeight / scale;

                int pageCount =
                        Math.max(
                                1,
                                (int) Math.ceil(
                                        preferredSize.height
                                                / pageBodyHeightInComponentUnits
                                )
                        );

                if (pageIndex >= pageCount) {
                    return Printable.NO_SUCH_PAGE;
                }

                double pageX =
                        format.getImageableX();

                double pageY =
                        format.getImageableY();

                drawHeader(
                        graphics2D,
                        format,
                        pageIndex,
                        pageCount
                );

                drawFooter(
                        graphics2D,
                        format,
                        pageIndex,
                        pageCount
                );

                graphics2D.translate(
                        pageX,
                        pageY + HEADER_HEIGHT
                );

                graphics2D.clip(
                        new Rectangle(
                                0,
                                0,
                                (int) Math.ceil(printableWidth),
                                (int) Math.ceil(printableBodyHeight)
                        )
                );

                graphics2D.scale(scale, scale);

                graphics2D.translate(
                        0,
                        -pageIndex
                                * pageBodyHeightInComponentUnits
                );

                reportPane.printAll(graphics2D);

                return Printable.PAGE_EXISTS;

            } finally {
                graphics2D.dispose();
            }
        };
    }

    private void drawHeader(
            Graphics2D graphics,
            PageFormat format,
            int pageIndex,
            int pageCount
    ) {
        double x = format.getImageableX();
        double y = format.getImageableY();
        double width = format.getImageableWidth();

        graphics.setColor(Color.BLACK);
        graphics.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        10
                )
        );

        String title =
                "RMA Report - "
                        + safeText(
                        rmaRecord.getRmaNumber()
                );

        FontMetrics metrics =
                graphics.getFontMetrics();

        graphics.drawString(
                title,
                (float) x,
                (float) (y + metrics.getAscent())
        );

        String pageText =
                "Page "
                        + (pageIndex + 1)
                        + " of "
                        + pageCount;

        float pageTextX =
                (float) (
                        x
                                + width
                                - metrics.stringWidth(pageText)
                );

        graphics.drawString(
                pageText,
                pageTextX,
                (float) (y + metrics.getAscent())
        );

        graphics.drawLine(
                (int) x,
                (int) (y + HEADER_HEIGHT - 6),
                (int) (x + width),
                (int) (y + HEADER_HEIGHT - 6)
        );
    }

    private void drawFooter(
            Graphics2D graphics,
            PageFormat format,
            int pageIndex,
            int pageCount
    ) {
        double x = format.getImageableX();
        double y =
                format.getImageableY()
                        + format.getImageableHeight()
                        - FOOTER_HEIGHT;

        double width = format.getImageableWidth();

        graphics.setColor(Color.DARK_GRAY);
        graphics.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        9
                )
        );

        graphics.drawLine(
                (int) x,
                (int) y,
                (int) (x + width),
                (int) y
        );

        String generatedText =
                "Generated "
                        + LocalDate.now()
                        .format(DATE_FORMATTER);

        FontMetrics metrics =
                graphics.getFontMetrics();

        graphics.drawString(
                generatedText,
                (float) x,
                (float) (y + metrics.getAscent() + 5)
        );

        String footerPageText =
                "RMA "
                        + safeText(rmaRecord.getRmaNumber())
                        + " | "
                        + (pageIndex + 1)
                        + "/"
                        + pageCount;

        float footerTextX =
                (float) (
                        x
                                + width
                                - metrics.stringWidth(
                                footerPageText
                        )
                );

        graphics.drawString(
                footerPageText,
                footerTextX,
                (float) (y + metrics.getAscent() + 5)
        );
    }

    private void showPrintError(
            String message,
            Exception exception
    ) {
        JOptionPane.showMessageDialog(
                this,
                message
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + exception.getMessage(),
                "Printing Error",
                JOptionPane.ERROR_MESSAGE
        );
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
        appendShippingInformation(html);
        appendMachineInformation(html);
        appendStatusHistory(html);

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
                "General Notes",
                formatMultilineText(
                        rmaRecord.getNotes()
                )
        );

        html.append("</table>");
    }

    private void appendShippingInformation(
            StringBuilder html
    ) {
        html.append("<h2>Shipping Information</h2>");

        List<ShippingInfo> shipments =
                rmaRecord.getShippingInformation();

        if (shipments == null || shipments.isEmpty()) {
            html.append("""
                    <div class='empty-message'>
                        No shipping information has been added.
                    </div>
                    """);

            return;
        }

        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Direction</th>");
        html.append("<th>Carrier</th>");
        html.append("<th>Shipping Number</th>");
        html.append("</tr>");

        for (ShippingInfo shipment : shipments) {
            html.append("<tr>");

            appendMachineCell(
                    html,
                    shipment.getDirection() == null
                            ? ""
                            : shipment.getDirection().toString()
            );

            appendMachineCell(
                    html,
                    shipment.getCarrier()
            );

            appendMachineCell(
                    html,
                    shipment.getTrackingNumber()
            );

            html.append("</tr>");
        }

        html.append("</table>");
    }

    private void appendMachineInformation(
            StringBuilder html
    ) {
        html.append(
                "<h2>Machines Included - Table View</h2>"
        );

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
        html.append("<th>Received</th>");
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

            appendMachineCell(
                    html,
                    repairItem.isReceived()
                            ? "X"
                            : ""
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

    private void appendStatusHistory(
            StringBuilder html
    ) {
        html.append("<h2>Status History</h2>");

        try {
            List<StatusHistory> historyEntries =
                    statusHistoryRepository.findByRmaNumber(
                            rmaRecord.getRmaNumber()
                    );

            if (historyEntries.isEmpty()) {
                html.append("""
                        <div class='empty-message'>
                            No status history was found for this RMA.
                        </div>
                        """);

                return;
            }

            html.append("<table>");
            html.append("<tr>");
            html.append("<th>Changed At</th>");
            html.append("<th>Previous Status</th>");
            html.append("<th>New Status</th>");
            html.append("</tr>");

            for (StatusHistory history : historyEntries) {
                html.append("<tr>");

                appendMachineCell(
                        html,
                        history.getChangedAt() == null
                                ? ""
                                : history.getChangedAt()
                                  .format(HISTORY_DATE_FORMATTER)
                );

                appendMachineCell(
                        html,
                        history.getOldStatus() == null
                                ? "Initial Status"
                                : history.getOldStatus().toString()
                );

                appendMachineCell(
                        html,
                        history.getNewStatus() == null
                                ? ""
                                : history.getNewStatus().toString()
                );

                html.append("</tr>");
            }

            html.append("</table>");

        } catch (SQLException exception) {
            html.append("""
                    <div class='empty-message'>
                        The status history could not be loaded.
                    </div>
                    """);
        }
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
