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
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Separate page-by-page print preview for one selected RMA.
 *
 * Pages are built as complete report sections, so tables and headings
 * are not cut in the middle of a printed page.
 */
public class RmaPrintPreviewWindow extends JDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter HISTORY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private static final double POINTS_PER_INCH = 72.0;
    private static final double DEFAULT_MARGIN = 0.50 * POINTS_PER_INCH;
    private static final double HEADER_HEIGHT = 28.0;
    private static final double FOOTER_HEIGHT = 24.0;

    private static final int MACHINES_PER_PAGE = 10;
    private static final int HISTORY_ROWS_PER_PAGE = 12;

    private final RmaRecord rmaRecord;
    private final StatusHistoryRepository statusHistoryRepository;
    private final JEditorPane pagePane;
    private final PreviewCanvas previewCanvas;
    private final JLabel pageLabel;
    private final JButton previousButton;
    private final JButton nextButton;
    private final JComboBox<String> zoomComboBox;

    private final List<String> pageHtml = new ArrayList<>();

    private PageFormat pageFormat;
    private int currentPageIndex;
    private double previewZoom = 1.0;

    public RmaPrintPreviewWindow(
            Window owner,
            RmaRecord rmaRecord
    ) {
        super(
                owner,
                "RMA Print Preview",
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

        pagePane = createPagePane();
        previewCanvas = new PreviewCanvas();
        pageLabel = new JLabel("Page 1 of 1");
        previousButton = new JButton("◀ Previous");
        nextButton = new JButton("Next ▶");

        zoomComboBox = new JComboBox<>(
                new String[]{
                        "50%",
                        "75%",
                        "100%",
                        "125%",
                        "150%",
                        "200%",
                        "Fit Page",
                        "Fit Width"
                }
        );

        zoomComboBox.setSelectedItem("100%");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 820);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(owner);

        setContentPane(createMainPanel());

        rebuildPages();

        SwingUtilities.invokeLater(
                this::applySelectedZoom
        );
    }

    private JPanel createMainPanel() {
        JPanel mainPanel =
                new JPanel(new BorderLayout());

        mainPanel.setBackground(
                new Color(48, 53, 61)
        );

        mainPanel.add(
                createHeaderPanel(),
                BorderLayout.NORTH
        );

        JScrollPane scrollPane =
                new JScrollPane(previewCanvas);

        scrollPane.getViewport().setBackground(
                new Color(78, 83, 92)
        );

        scrollPane.getVerticalScrollBar()
                .setUnitIncrement(24);

        scrollPane.getHorizontalScrollBar()
                .setUnitIncrement(24);

        previewCanvas.setScrollPane(scrollPane);

        mainPanel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        mainPanel.add(
                createFooterPanel(),
                BorderLayout.SOUTH
        );

        return mainPanel;
    }

    private JPanel createHeaderPanel() {
        JPanel panel =
                new JPanel(new BorderLayout(12, 6));

        panel.setBackground(
                new Color(30, 64, 104)
        );

        panel.setBorder(
                new EmptyBorder(12, 16, 12, 16)
        );

        JLabel title = new JLabel(
                "PRINT PREVIEW — RMA "
                        + safeText(rmaRecord.getRmaNumber())
        );

        title.setForeground(Color.WHITE);
        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        JPanel buttons =
                new JPanel(new FlowLayout(
                        FlowLayout.RIGHT,
                        8,
                        0
                ));

        buttons.setOpaque(false);

        JButton pageSetupButton =
                new JButton("Page Setup...");

        JButton printButton =
                new JButton("Print...");

        JButton closeButton =
                new JButton("Close Preview");

        pageSetupButton.addActionListener(
                event -> showPageSetup()
        );

        printButton.addActionListener(
                event -> printReport()
        );

        closeButton.addActionListener(
                event -> dispose()
        );

        buttons.add(pageSetupButton);
        buttons.add(printButton);
        buttons.add(closeButton);

        panel.add(title, BorderLayout.WEST);
        panel.add(buttons, BorderLayout.EAST);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel =
                new JPanel(new BorderLayout());

        panel.setBackground(
                new Color(52, 58, 67)
        );

        panel.setBorder(
                new EmptyBorder(9, 12, 9, 12)
        );

        JPanel navigation =
                new JPanel(new FlowLayout(
                        FlowLayout.LEFT,
                        8,
                        0
                ));

        navigation.setOpaque(false);

        previousButton.addActionListener(
                event -> showPreviousPage()
        );

        nextButton.addActionListener(
                event -> showNextPage()
        );

        pageLabel.setForeground(Color.WHITE);
        pageLabel.setFont(
                pageLabel.getFont()
                        .deriveFont(Font.BOLD)
        );

        navigation.add(previousButton);
        navigation.add(nextButton);
        navigation.add(pageLabel);

        JPanel zoom =
                new JPanel(new FlowLayout(
                        FlowLayout.CENTER,
                        8,
                        0
                ));

        zoom.setOpaque(false);

        JLabel zoomLabel = new JLabel("Zoom:");
        zoomLabel.setForeground(Color.WHITE);

        zoomComboBox.addActionListener(
                event -> applySelectedZoom()
        );

        zoom.add(zoomLabel);
        zoom.add(zoomComboBox);

        panel.add(navigation, BorderLayout.WEST);
        panel.add(zoom, BorderLayout.CENTER);

        return panel;
    }

    private JEditorPane createPagePane() {
        JEditorPane pane = new JEditorPane();

        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setEditorKit(new HTMLEditorKit());
        pane.setBackground(Color.WHITE);

        pane.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                Boolean.TRUE
        );

        return pane;
    }

    private PageFormat createDefaultPageFormat() {
        PrinterJob printerJob =
                PrinterJob.getPrinterJob();

        PageFormat format =
                printerJob.defaultPage();

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

    private void rebuildPages() {
        pageHtml.clear();

        pageHtml.add(buildSummaryPage());

        List<RepairItem> machines =
                rmaRecord.getRepairItems();

        if (machines != null && !machines.isEmpty()) {
            for (
                    int start = 0;
                    start < machines.size();
                    start += MACHINES_PER_PAGE
            ) {
                int end = Math.min(
                        start + MACHINES_PER_PAGE,
                        machines.size()
                );

                pageHtml.add(
                        buildMachinePage(
                                machines.subList(start, end),
                                start,
                                machines.size()
                        )
                );
            }
        }

        List<StatusHistory> history =
                loadStatusHistory();

        if (!history.isEmpty()) {
            for (
                    int start = 0;
                    start < history.size();
                    start += HISTORY_ROWS_PER_PAGE
            ) {
                int end = Math.min(
                        start + HISTORY_ROWS_PER_PAGE,
                        history.size()
                );

                pageHtml.add(
                        buildStatusHistoryPage(
                                history.subList(start, end),
                                start,
                                history.size()
                        )
                );
            }
        } else {
            pageHtml.add(buildEmptyHistoryPage());
        }

        currentPageIndex = Math.min(
                currentPageIndex,
                pageHtml.size() - 1
        );

        showCurrentPage();
    }

    private String buildSummaryPage() {
        StringBuilder html =
                beginPageHtml("RMA Report");

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

        html.append("<h2>Shipping Information</h2>");

        List<ShippingInfo> shipping =
                rmaRecord.getShippingInformation();

        if (shipping == null || shipping.isEmpty()) {
            html.append("""
                    <div class='empty-message'>
                        No shipping information has been added.
                    </div>
                    """);
        } else {
            html.append("<table>");
            html.append("<tr>");
            html.append("<th>Direction</th>");
            html.append("<th>Carrier</th>");
            html.append("<th>Tracking Number</th>");
            html.append("</tr>");

            for (ShippingInfo shipment : shipping) {
                html.append("<tr>");

                appendCell(
                        html,
                        shipment.getDirection() == null
                                ? ""
                                : shipment.getDirection().toString()
                );

                appendCell(
                        html,
                        shipment.getCarrier()
                );

                appendCell(
                        html,
                        shipment.getTrackingNumber()
                );

                html.append("</tr>");
            }

            html.append("</table>");
        }

        int machineCount =
                rmaRecord.getRepairItems() == null
                        ? 0
                        : rmaRecord.getRepairItems().size();

        html.append("<p class='summary'>")
                .append("<strong>Total Machines:</strong> ")
                .append(machineCount)
                .append("</p>");

        return endPageHtml(html);
    }

    private String buildMachinePage(
            List<RepairItem> machines,
            int startIndex,
            int totalMachines
    ) {
        StringBuilder html =
                beginPageHtml("Machines Included");

        html.append("<p class='summary'>")
                .append("Machines ")
                .append(startIndex + 1)
                .append("–")
                .append(startIndex + machines.size())
                .append(" of ")
                .append(totalMachines)
                .append("</p>");

        html.append("<table>");
        html.append("<tr>");
        html.append("<th>County</th>");
        html.append("<th>Serial Number</th>");
        html.append("<th>Equipment Type</th>");
        html.append("<th>Problem</th>");
        html.append("<th>Repair</th>");
        html.append("<th>Received</th>");
        html.append("</tr>");

        for (RepairItem machine : machines) {
            html.append("<tr>");

            appendCell(html, machine.getCounty());
            appendCell(html, machine.getSerialNumber());
            appendCell(html, machine.getMachineType());
            appendCell(html, machine.getProblemDescription());
            appendCell(html, machine.getRepairDescription());

            appendCell(
                    html,
                    machine.isReceived()
                            ? "Yes"
                            : "No"
            );

            html.append("</tr>");
        }

        html.append("</table>");

        return endPageHtml(html);
    }

    private String buildStatusHistoryPage(
            List<StatusHistory> entries,
            int startIndex,
            int totalEntries
    ) {
        StringBuilder html =
                beginPageHtml("Status History");

        html.append("<p class='summary'>")
                .append("History entries ")
                .append(startIndex + 1)
                .append("–")
                .append(startIndex + entries.size())
                .append(" of ")
                .append(totalEntries)
                .append("</p>");

        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Changed At</th>");
        html.append("<th>Previous Status</th>");
        html.append("<th>New Status</th>");
        html.append("</tr>");

        for (StatusHistory history : entries) {
            html.append("<tr>");

            appendCell(
                    html,
                    history.getChangedAt() == null
                            ? ""
                            : history.getChangedAt()
                            .format(HISTORY_DATE_FORMATTER)
            );

            appendCell(
                    html,
                    history.getOldStatus() == null
                            ? "Initial Status"
                            : history.getOldStatus().toString()
            );

            appendCell(
                    html,
                    history.getNewStatus() == null
                            ? ""
                            : history.getNewStatus().toString()
            );

            html.append("</tr>");
        }

        html.append("</table>");

        return endPageHtml(html);
    }

    private String buildEmptyHistoryPage() {
        StringBuilder html =
                beginPageHtml("Status History");

        html.append("""
                <div class='empty-message'>
                    No status history was found for this RMA.
                </div>
                """);

        return endPageHtml(html);
    }

    private List<StatusHistory> loadStatusHistory() {
        try {
            return statusHistoryRepository.findByRmaNumber(
                    rmaRecord.getRmaNumber()
            );
        } catch (SQLException exception) {
            return List.of();
        }
    }

    private StringBuilder beginPageHtml(
            String sectionTitle
    ) {
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
                            margin: 0 0 4px 0;
                        }

                        h2 {
                            color: #17365d;
                            border-bottom: 2px solid #7f9db9;
                            padding-bottom: 4px;
                            margin-top: 22px;
                        }

                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 10px;
                        }

                        th {
                            background-color: #d9eaf7;
                            text-align: left;
                            padding: 7px;
                            border: 1px solid #999999;
                        }

                        td {
                            padding: 7px;
                            border: 1px solid #999999;
                            vertical-align: top;
                        }

                        .label {
                            font-weight: bold;
                            width: 175px;
                            background-color: #f1f1f1;
                        }

                        .empty-message {
                            padding: 14px;
                            background-color: #fff4cc;
                            border: 1px solid #d6b656;
                        }

                        .summary {
                            margin-top: 12px;
                        }

                        .report-number {
                            margin-top: 0;
                            color: #555555;
                        }
                    </style>
                </head>
                <body>
                """);

        html.append("<h1>")
                .append(htmlEscape(sectionTitle))
                .append("</h1>");

        html.append("<p class='report-number'>RMA ")
                .append(
                        htmlEscape(
                                safeText(
                                        rmaRecord.getRmaNumber()
                                )
                        )
                )
                .append("</p>");

        return html;
    }

    private String endPageHtml(
            StringBuilder html
    ) {
        html.append("</body></html>");
        return html.toString();
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

    private void appendCell(
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

    private void showCurrentPage() {
        if (pageHtml.isEmpty()) {
            return;
        }

        pagePane.setText(
                pageHtml.get(currentPageIndex)
        );

        pagePane.setCaretPosition(0);

        layoutPagePane();

        pageLabel.setText(
                "Page "
                        + (currentPageIndex + 1)
                        + " of "
                        + pageHtml.size()
        );

        previousButton.setEnabled(
                currentPageIndex > 0
        );

        nextButton.setEnabled(
                currentPageIndex < pageHtml.size() - 1
        );

        previewCanvas.refreshPage();
    }

    private void layoutPagePane() {
        int width = Math.max(
                1,
                (int) Math.floor(
                        pageFormat.getImageableWidth()
                )
        );

        pagePane.setSize(
                width,
                Integer.MAX_VALUE
        );

        Dimension preferred =
                pagePane.getPreferredSize();

        pagePane.setSize(
                width,
                Math.max(
                        1,
                        preferred.height
                )
        );

        pagePane.doLayout();
    }

    private void showPreviousPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            showCurrentPage();
        }
    }

    private void showNextPage() {
        if (currentPageIndex < pageHtml.size() - 1) {
            currentPageIndex++;
            showCurrentPage();
        }
    }

    private void showPageSetup() {
        PrinterJob printerJob =
                PrinterJob.getPrinterJob();

        PageFormat selected =
                printerJob.pageDialog(pageFormat);

        if (selected != null) {
            pageFormat =
                    printerJob.validatePage(selected);

            showCurrentPage();
            applySelectedZoom();
        }
    }

    private void printReport() {
        PrinterJob printerJob =
                PrinterJob.getPrinterJob();

        printerJob.setJobName(
                "RMA Report - "
                        + safeText(rmaRecord.getRmaNumber())
        );

        printerJob.setPrintable(
                createPrintable(),
                pageFormat
        );

        try {
            if (printerJob.printDialog()) {
                printerJob.print();
            }
        } catch (PrinterException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The report could not be printed."
                            + System.lineSeparator()
                            + exception.getMessage(),
                    "Printing Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private Printable createPrintable() {
        return (graphics, format, pageIndex) -> {
            if (
                    pageIndex < 0
                            || pageIndex >= pageHtml.size()
            ) {
                return Printable.NO_SUCH_PAGE;
            }

            String originalHtml =
                    pagePane.getText();

            Dimension originalSize =
                    pagePane.getSize();

            Graphics2D graphics2D =
                    (Graphics2D) graphics.create();

            try {
                pagePane.setText(
                        pageHtml.get(pageIndex)
                );

                layoutPagePane();

                renderPage(
                        graphics2D,
                        format,
                        pageIndex
                );

                return Printable.PAGE_EXISTS;
            } finally {
                pagePane.setText(originalHtml);
                pagePane.setSize(originalSize);
                graphics2D.dispose();
            }
        };
    }

    private void renderPage(
            Graphics2D graphics,
            PageFormat format,
            int pageIndex
    ) {
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        drawHeader(
                graphics,
                format,
                pageIndex
        );

        drawFooter(
                graphics,
                format,
                pageIndex
        );

        double x =
                format.getImageableX();

        double y =
                format.getImageableY()
                        + HEADER_HEIGHT;

        double bodyHeight =
                format.getImageableHeight()
                        - HEADER_HEIGHT
                        - FOOTER_HEIGHT;

        Shape oldClip =
                graphics.getClip();

        graphics.translate(x, y);

        graphics.clip(
                new Rectangle(
                        0,
                        0,
                        (int) Math.ceil(
                                format.getImageableWidth()
                        ),
                        (int) Math.ceil(bodyHeight)
                )
        );

        pagePane.printAll(graphics);

        graphics.setClip(oldClip);
    }

    private void drawHeader(
            Graphics2D graphics,
            PageFormat format,
            int pageIndex
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
                        + pageHtml.size();

        graphics.drawString(
                pageText,
                (float) (
                        x
                                + width
                                - metrics.stringWidth(pageText)
                ),
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
            int pageIndex
    ) {
        double x = format.getImageableX();

        double y =
                format.getImageableY()
                        + format.getImageableHeight()
                        - FOOTER_HEIGHT;

        double width =
                format.getImageableWidth();

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

        String generated =
                "Generated "
                        + LocalDate.now()
                        .format(DATE_FORMATTER);

        FontMetrics metrics =
                graphics.getFontMetrics();

        graphics.drawString(
                generated,
                (float) x,
                (float) (
                        y
                                + metrics.getAscent()
                                + 5
                )
        );

        String pageText =
                "RMA "
                        + safeText(rmaRecord.getRmaNumber())
                        + " | "
                        + (pageIndex + 1)
                        + "/"
                        + pageHtml.size();

        graphics.drawString(
                pageText,
                (float) (
                        x
                                + width
                                - metrics.stringWidth(pageText)
                ),
                (float) (
                        y
                                + metrics.getAscent()
                                + 5
                )
        );
    }

    private BufferedImage renderPreviewImage() {
        int width = Math.max(
                1,
                (int) Math.ceil(
                        pageFormat.getWidth()
                )
        );

        int height = Math.max(
                1,
                (int) Math.ceil(
                        pageFormat.getHeight()
                )
        );

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(
                    0,
                    0,
                    width,
                    height
            );

            renderPage(
                    graphics,
                    pageFormat,
                    currentPageIndex
            );
        } finally {
            graphics.dispose();
        }

        return image;
    }

    private void applySelectedZoom() {
        Object selected =
                zoomComboBox.getSelectedItem();

        if (selected == null) {
            return;
        }

        switch (selected.toString()) {
            case "50%" -> previewZoom = 0.50;
            case "75%" -> previewZoom = 0.75;
            case "100%" -> previewZoom = 1.00;
            case "125%" -> previewZoom = 1.25;
            case "150%" -> previewZoom = 1.50;
            case "200%" -> previewZoom = 2.00;
            case "Fit Page" ->
                    previewZoom =
                            previewCanvas.calculateFitPageZoom();
            case "Fit Width" ->
                    previewZoom =
                            previewCanvas.calculateFitWidthZoom();
            default -> previewZoom = 1.00;
        }

        previewCanvas.updatePreferredSize();
        previewCanvas.repaint();
    }

    private String formatDate(LocalDate date) {
        return date == null
                ? ""
                : htmlEscape(
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

    private final class PreviewCanvas
            extends JPanel {

        private BufferedImage image;
        private JScrollPane scrollPane;

        private PreviewCanvas() {
            setBackground(
                    new Color(78, 83, 92)
            );

            setBorder(
                    new EmptyBorder(30, 30, 30, 30)
            );
        }

        private void setScrollPane(
                JScrollPane scrollPane
        ) {
            this.scrollPane = scrollPane;
        }

        private void refreshPage() {
            image = renderPreviewImage();
            updatePreferredSize();
            repaint();
        }

        private void updatePreferredSize() {
            int width = Math.max(
                    1,
                    (int) Math.round(
                            pageFormat.getWidth()
                                    * previewZoom
                    )
            );

            int height = Math.max(
                    1,
                    (int) Math.round(
                            pageFormat.getHeight()
                                    * previewZoom
                    )
            );

            setPreferredSize(
                    new Dimension(
                            width + 80,
                            height + 80
                    )
            );

            revalidate();
        }

        private double calculateFitPageZoom() {
            if (scrollPane == null) {
                return 1.0;
            }

            Dimension viewport =
                    scrollPane.getViewport()
                            .getExtentSize();

            double widthZoom =
                    (viewport.width - 80)
                            / pageFormat.getWidth();

            double heightZoom =
                    (viewport.height - 80)
                            / pageFormat.getHeight();

            return clamp(
                    Math.min(
                            widthZoom,
                            heightZoom
                    )
            );
        }

        private double calculateFitWidthZoom() {
            if (scrollPane == null) {
                return 1.0;
            }

            Dimension viewport =
                    scrollPane.getViewport()
                            .getExtentSize();

            return clamp(
                    (viewport.width - 80)
                            / pageFormat.getWidth()
            );
        }

        private double clamp(double value) {
            return Math.max(
                    0.25,
                    Math.min(3.0, value)
            );
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        ) {
            super.paintComponent(graphics);

            if (image == null) {
                return;
            }

            Graphics2D graphics2D =
                    (Graphics2D) graphics.create();

            try {
                graphics2D.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR
                );

                int width = Math.max(
                        1,
                        (int) Math.round(
                                image.getWidth()
                                        * previewZoom
                        )
                );

                int height = Math.max(
                        1,
                        (int) Math.round(
                                image.getHeight()
                                        * previewZoom
                        )
                );

                int x = Math.max(
                        30,
                        (getWidth() - width) / 2
                );

                int y = 30;

                graphics2D.setColor(
                        new Color(25, 25, 25, 120)
                );

                graphics2D.fillRect(
                        x + 7,
                        y + 7,
                        width,
                        height
                );

                graphics2D.drawImage(
                        image,
                        x,
                        y,
                        width,
                        height,
                        null
                );

                graphics2D.setColor(
                        new Color(185, 185, 185)
                );

                graphics2D.drawRect(
                        x,
                        y,
                        width,
                        height
                );
            } finally {
                graphics2D.dispose();
            }
        }
    }
}
