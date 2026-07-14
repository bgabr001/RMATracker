package com.harpenterprises.rmatracker.report;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.ShippingInfo;
import com.harpenterprises.rmatracker.model.Status;
import com.harpenterprises.rmatracker.model.StatusHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/** Builds the professional HTML used by the RMA report preview. */
public final class RmaReportHtmlBuilder {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private static final DateTimeFormatter HISTORY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");

    /**
     * Builds the report without status history.
     * This overload keeps older code compatible.
     */
    public String build(RmaRecord record) {
        return build(record, Collections.emptyList());
    }

    /** Builds a complete report containing the RMA and its status history. */
    public String build(
            RmaRecord record,
            List<StatusHistory> statusHistory
    ) {
        if (record == null) {
            throw new IllegalArgumentException("RMA record cannot be null.");
        }

        List<StatusHistory> safeHistory = statusHistory == null
                ? Collections.emptyList()
                : statusHistory;

        StringBuilder html = new StringBuilder();
        appendDocumentStart(html);
        appendReportHeader(html, record);
        appendSummarySection(html, record);
        appendNotesSection(html, record);
        appendShippingSection(html, record);
        appendMachinesSection(html, record);
        appendStatusHistorySection(html, safeHistory);
        appendFooter(html);
        html.append("</div></body></html>");

        return html.toString();
    }

    private void appendDocumentStart(StringBuilder html) {
        html.append("""
                <html>
                <head>
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        background-color: #e9edf2;
                        color: #202938;
                        font-family: Arial, Helvetica, sans-serif;
                        font-size: 12px;
                    }

                    .report-page {
                        width: 94%;
                        margin: 18px auto;
                        padding: 0 0 24px 0;
                        background-color: #ffffff;
                        border: 1px solid #c9d1dc;
                    }

                    .report-header {
                        background-color: #17365d;
                        color: #ffffff;
                        padding: 24px 28px;
                    }

                    .company-name {
                        margin: 0;
                        font-size: 15px;
                        font-weight: bold;
                        letter-spacing: 1px;
                    }

                    .report-title {
                        margin: 5px 0 0 0;
                        font-size: 28px;
                        font-weight: bold;
                    }

                    .report-subtitle {
                        margin: 7px 0 0 0;
                        color: #dce8f5;
                        font-size: 12px;
                    }

                    .content {
                        padding: 22px 28px 0 28px;
                    }

                    .section {
                        margin-top: 22px;
                    }

                    .section-title {
                        margin: 0 0 10px 0;
                        padding: 8px 11px;
                        background-color: #d9e8f5;
                        border-left: 5px solid #2f75b5;
                        color: #17365d;
                        font-size: 16px;
                        font-weight: bold;
                    }

                    table {
                        width: 100%;
                        border-collapse: collapse;
                    }

                    td, th {
                        border: 1px solid #c6ced8;
                        padding: 8px;
                        vertical-align: top;
                    }

                    th {
                        background-color: #edf3f8;
                        color: #17365d;
                        font-weight: bold;
                        text-align: left;
                    }

                    .machine-table-wrap {
                        width: 100%;
                        overflow-x: auto;
                    }

                    .machine-table {
                        table-layout: fixed;
                        font-size: 10px;
                    }

                    .machine-table th,
                    .machine-table td {
                        padding: 6px;
                        line-height: 1.25;
                        overflow-wrap: break-word;
                    }

                    .machine-table th:nth-child(1) { width: 3%; }
                    .machine-table th:nth-child(2) { width: 10%; }
                    .machine-table th:nth-child(3) { width: 11%; }
                    .machine-table th:nth-child(4) { width: 12%; }
                    .machine-table th:nth-child(5) { width: 7%; }
                    .machine-table th:nth-child(6) { width: 22%; }
                    .machine-table th:nth-child(7) { width: 22%; }
                    .machine-table th:nth-child(8) { width: 8%; }

                    .received-cell {
                        text-align: center;
                        white-space: nowrap;
                    }

                    .summary-label {
                        width: 22%;
                        background-color: #f3f6f9;
                        color: #4b5563;
                        font-size: 10px;
                        font-weight: bold;
                        text-transform: uppercase;
                    }

                    .summary-value {
                        width: 28%;
                        font-size: 13px;
                        font-weight: bold;
                    }

                    .machine-count {
                        display: inline-block;
                        padding: 5px 11px;
                        background-color: #17365d;
                        color: #ffffff;
                        font-weight: bold;
                    }

                    .notes-box, .empty-message {
                        padding: 12px;
                        border: 1px solid #c6ced8;
                        background-color: #fafbfd;
                        line-height: 1.45;
                    }

                    .empty-message {
                        color: #687386;
                        font-style: italic;
                    }

                    .received-yes {
                        color: #1c6b3c;
                        font-weight: bold;
                    }

                    .received-no {
                        color: #9b2c2c;
                        font-weight: bold;
                    }

                    .history-date {
                        width: 31%;
                        white-space: nowrap;
                    }

                    .status-arrow {
                        color: #687386;
                        text-align: center;
                        width: 6%;
                    }

                    .footer {
                        margin: 28px 28px 0 28px;
                        padding-top: 10px;
                        border-top: 1px solid #c6ced8;
                        color: #687386;
                        font-size: 10px;
                        text-align: center;
                    }

                    @media print {
                        body {
                            background-color: #ffffff;
                        }

                        .report-page {
                            width: 100%;
                            margin: 0;
                            border: none;
                        }

                        .section-title {
                            page-break-after: avoid;
                        }
                    }
                </style>
                </head>
                <body>
                <div class='report-page'>
                """);
    }

    private void appendReportHeader(StringBuilder html, RmaRecord record) {
        html.append("<div class='report-header'>")
                .append("<p class='company-name'>HARP ENTERPRISES</p>")
                .append("<p class='report-title'>RMA Repair Report</p>")
                .append("<p class='report-subtitle'>RMA ")
                .append(htmlEscape(safeText(record.getRmaNumber())))
                .append(" &nbsp;&bull;&nbsp; Generated ")
                .append(LocalDate.now().format(DATE_FORMATTER))
                .append("</p></div>")
                .append("<div class='content'>");
    }

    private void appendSummarySection(StringBuilder html, RmaRecord record) {
        int machineCount = record.getRepairItems() == null
                ? 0
                : record.getRepairItems().size();

        html.append("<div class='section'>")
                .append("<div class='section-title'>Report Summary</div>")
                .append("<table>");

        html.append("<tr>");
        appendSummaryCell(html, "RMA Number", safeText(record.getRmaNumber()));
        appendSummaryCell(html, "Current Status", formatStatus(record.getStatus()));
        html.append("</tr>");

        html.append("<tr>");
        appendSummaryCell(html, "Date Sent", formatDate(record.getDateSent()));
        appendSummaryCell(html, "Date Received", formatDate(record.getDateReceived()));
        html.append("</tr>");

        html.append("<tr>")
                .append("<td class='summary-label'>Machine Count</td>")
                .append("<td class='summary-value' colspan='3'>")
                .append("<span class='machine-count'>")
                .append(machineCount)
                .append(machineCount == 1 ? " Machine" : " Machines")
                .append("</span></td></tr>")
                .append("</table></div>");
    }

    private void appendNotesSection(StringBuilder html, RmaRecord record) {
        html.append("<div class='section'>")
                .append("<div class='section-title'>General Notes</div>");

        if (record.getNotes() == null || record.getNotes().isBlank()) {
            html.append("<div class='empty-message'>No general notes have been added.</div>");
        } else {
            html.append("<div class='notes-box'>")
                    .append(formatMultilineText(record.getNotes()))
                    .append("</div>");
        }

        html.append("</div>");
    }

    private void appendShippingSection(StringBuilder html, RmaRecord record) {
        html.append("<div class='section'>")
                .append("<div class='section-title'>Shipping Information</div>");

        List<ShippingInfo> shipments = record.getShippingInformation();
        if (shipments == null || shipments.isEmpty()) {
            html.append("<div class='empty-message'>No shipping information has been added.</div></div>");
            return;
        }

        html.append("<table><tr>")
                .append("<th>Direction</th>")
                .append("<th>Carrier</th>")
                .append("<th>Tracking Number</th>")
                .append("</tr>");

        for (ShippingInfo shipment : shipments) {
            html.append("<tr>");
            appendCell(html, shipment.getDirection() == null
                    ? ""
                    : formatStatusText(shipment.getDirection().toString()));
            appendCell(html, shipment.getCarrier());
            appendCell(html, shipment.getTrackingNumber());
            html.append("</tr>");
        }

        html.append("</table></div>");
    }

    private void appendMachinesSection(StringBuilder html, RmaRecord record) {
        html.append("<div class='section'>")
                .append("<div class='section-title'>Machines Included</div>");

        List<RepairItem> items = record.getRepairItems();
        if (items == null || items.isEmpty()) {
            html.append("<div class='empty-message'>No machines are attached to this RMA.</div></div>");
            return;
        }

        html.append("<div class='machine-table-wrap'><table class='machine-table'><tr>")
                .append("<th>#</th>")
                .append("<th>County</th>")
                .append("<th>Equipment Type</th>")
                .append("<th>Serial Number</th>")
                .append("<th>Version</th>")
                .append("<th>Problem Description</th>")
                .append("<th>Repair Description</th>")
                .append("<th>Received</th>")
                .append("</tr>");

        for (int index = 0; index < items.size(); index++) {
            RepairItem item = items.get(index);
            html.append("<tr>");
            appendCell(html, String.valueOf(index + 1));
            appendCell(html, item.getCounty());
            appendCell(html, item.getMachineType());
            appendCell(html, item.getSerialNumber());
            appendCell(html, item.getVersion());
            appendCell(html, item.getProblemDescription());
            appendCell(html, item.getRepairDescription());
            html.append("<td class='received-cell'>")
                    .append(item.isReceived()
                            ? "<span class='received-yes'>YES</span>"
                            : "<span class='received-no'>NO</span>")
                    .append("</td></tr>");
        }

        html.append("</table></div></div>");
    }

    private void appendStatusHistorySection(
            StringBuilder html,
            List<StatusHistory> historyEntries
    ) {
        html.append("<div class='section'>")
                .append("<div class='section-title'>Status History</div>");

        if (historyEntries.isEmpty()) {
            html.append("<div class='empty-message'>No status changes have been recorded for this RMA.</div></div>");
            return;
        }

        html.append("<table><tr>")
                .append("<th>Changed At</th>")
                .append("<th>Previous Status</th>")
                .append("<th></th>")
                .append("<th>New Status</th>")
                .append("</tr>");

        for (StatusHistory history : historyEntries) {
            html.append("<tr>")
                    .append("<td class='history-date'>")
                    .append(formatDateTime(history.getChangedAt()))
                    .append("</td>")
                    .append("<td>")
                    .append(history.getOldStatus() == null
                            ? "Initial Status"
                            : htmlEscape(formatStatus(history.getOldStatus())))
                    .append("</td>")
                    .append("<td class='status-arrow'>&rarr;</td>")
                    .append("<td><strong>")
                    .append(htmlEscape(formatStatus(history.getNewStatus())))
                    .append("</strong></td></tr>");
        }

        html.append("</table></div>");
    }

    private void appendFooter(StringBuilder html) {
        html.append("</div>")
                .append("<div class='footer'>")
                .append("Harp Enterprises &nbsp;&bull;&nbsp; RMA Repair Report &nbsp;&bull;&nbsp; Generated ")
                .append(LocalDate.now().format(DATE_FORMATTER))
                .append("</div>");
    }

    private void appendSummaryCell(StringBuilder html, String label, String value) {
        html.append("<td class='summary-label'>")
                .append(htmlEscape(label))
                .append("</td><td class='summary-value'>")
                .append(value == null || value.isBlank()
                        ? "&mdash;"
                        : htmlEscape(value))
                .append("</td>");
    }

    private void appendDescriptionRow(StringBuilder html, String label, String value) {
        html.append("<tr><td class='description-label'>")
                .append(htmlEscape(label))
                .append("</td><td colspan='3'>")
                .append(value == null || value.isBlank()
                        ? "&mdash;"
                        : formatMultilineText(value))
                .append("</td></tr>");
    }

    private void appendCell(StringBuilder html, String value) {
        html.append("<td>")
                .append(value == null || value.isBlank()
                        ? "&mdash;"
                        : formatMultilineText(value))
                .append("</td>");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null
                ? "&mdash;"
                : htmlEscape(dateTime.format(HISTORY_DATE_FORMATTER));
    }

    private String formatStatus(Status status) {
        return status == null ? "" : formatStatusText(status.name());
    }

    private String formatStatusText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String[] words = value.trim().toLowerCase().replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }

        return result.toString();
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "Not entered" : value;
    }

    private String formatMultilineText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return htmlEscape(text)
                .replace("\r\n", "<br>")
                .replace("\n", "<br>")
                .replace("\r", "<br>");
    }

    private String safeText(Object value) {
        return value == null ? "" : value.toString();
    }

    private String htmlEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
