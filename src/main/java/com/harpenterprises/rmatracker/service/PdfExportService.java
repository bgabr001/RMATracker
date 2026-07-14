package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports selected and displayed RMAs to dependency-free PDF files.
 * The layout mirrors the Excel exports while grouping machines by RMA.
 */
public class PdfExportService {

    private static final DateTimeFormatter GENERATED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");

    private static final float PAGE_WIDTH = 792f;
    private static final float PAGE_HEIGHT = 612f;
    private static final float LEFT = 42f;
    private static final float RIGHT = 42f;
    private static final float TOP = 42f;
    private static final float BOTTOM = 42f;
    private static final float ROW_HEIGHT = 22f;

    private static final String[] HEADERS = {
            "County", "Serial Number", "Machine Type", "Version", "Received"
    };

    private static final float[] WIDTHS = {150f, 170f, 190f, 100f, 98f};

    public void exportSelectedRma(RmaRecord record, Path outputFile) throws IOException {
        if (record == null) {
            throw new IllegalArgumentException("RMA record cannot be null.");
        }
        validateOutputFile(outputFile);

        List<RmaRecord> records = new ArrayList<>();
        records.add(record);
        writePdf(records, outputFile, false);
    }

    /** The supplied records must be the already-filtered records displayed in MainWindow. */
    public void exportDisplayedRmas(List<RmaRecord> records, Path outputFile) throws IOException {
        if (records == null) {
            throw new IllegalArgumentException("Displayed RMA list cannot be null.");
        }
        validateOutputFile(outputFile);
        writePdf(records, outputFile, true);
    }

    private void validateOutputFile(Path outputFile) throws IOException {
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null.");
        }
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void writePdf(List<RmaRecord> records, Path outputFile, boolean displayedExport)
            throws IOException {
        List<StringBuilder> pageStreams = new ArrayList<>();
        PageCanvas canvas = new PageCanvas(pageStreams);

        canvas.newPage();
        canvas.title(displayedExport ? "DISPLAYED RMA MACHINES" : "RMA MACHINE REPORT");
        canvas.smallText("Generated: " + LocalDateTime.now().format(GENERATED_FORMAT));
        canvas.gap(12f);

        if (records.isEmpty()) {
            canvas.bodyText("No displayed RMAs were available to export.");
        } else {
            for (RmaRecord record : records) {
                if (record == null) {
                    continue;
                }
                drawRmaSection(canvas, record);
            }
        }

        int totalPages = pageStreams.size();
        for (int index = 0; index < totalPages; index++) {
            addFooter(pageStreams.get(index), index + 1, totalPages);
        }

        Files.write(outputFile, buildPdf(pageStreams));
    }

    private void drawRmaSection(PageCanvas canvas, RmaRecord record) {
        int itemCount = record.getRepairItems() == null ? 0 : record.getRepairItems().size();
        float required = 72f + Math.min(Math.max(itemCount, 1), 3) * ROW_HEIGHT;
        canvas.ensureSpace(required);

        canvas.sectionHeading("RMA Number: " + valueOrBlank(record.getRmaNumber()));
        canvas.smallText(
                "Date Sent: " + valueOrBlank(record.getDateSent())
                        + "    Date Received: " + valueOrBlank(record.getDateReceived())
                        + "    Status: " + valueOrBlank(record.getStatus())
        );
        canvas.gap(7f);
        canvas.tableHeader();

        List<RepairItem> items = record.getRepairItems();
        if (items == null || items.isEmpty()) {
            canvas.tableRow(new String[]{"No machines attached", "", "", "", ""});
        } else {
            for (RepairItem item : items) {
                if (item == null) {
                    continue;
                }
                if (!canvas.hasSpace(ROW_HEIGHT + 10f)) {
                    canvas.newPage();
                    canvas.sectionHeading("RMA Number: " + valueOrBlank(record.getRmaNumber()) + " (continued)");
                    canvas.tableHeader();
                }
                canvas.tableRow(new String[]{
                        valueOrBlank(item.getCounty()),
                        valueOrBlank(item.getSerialNumber()),
                        valueOrBlank(item.getMachineType()),
                        valueOrBlank(item.getVersion()),
                        item.isReceived() ? "X" : ""
                });
            }
        }
        canvas.gap(18f);
    }

    private byte[] buildPdf(List<StringBuilder> pages) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");

        int pageCount = pages.size();
        int fontObject = 3 + pageCount * 2;
        int totalObjects = fontObject;

        offsets.add(0);
        writeObject(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder kids = new StringBuilder("[");
        for (int i = 0; i < pageCount; i++) {
            kids.append(3 + i * 2).append(" 0 R ");
        }
        kids.append(']');
        writeObject(out, offsets, 2,
                "<< /Type /Pages /Kids " + kids + " /Count " + pageCount + " >>");

        for (int i = 0; i < pageCount; i++) {
            int pageObject = 3 + i * 2;
            int contentObject = pageObject + 1;
            writeObject(out, offsets, pageObject,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "] "
                            + "/Resources << /Font << /F1 " + fontObject + " 0 R >> >> "
                            + "/Contents " + contentObject + " 0 R >>");

            byte[] content = pages.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            offsets.add(out.size());
            write(out, contentObject + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            out.write(content);
            write(out, "\nendstream\nendobj\n");
        }

        writeObject(out, offsets, fontObject,
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        int xrefOffset = out.size();
        write(out, "xref\n0 " + (totalObjects + 1) + "\n");
        write(out, "0000000000 65535 f \n");
        for (int i = 1; i <= totalObjects; i++) {
            write(out, String.format("%010d 00000 n \n", offsets.get(i)));
        }
        write(out, "trailer\n<< /Size " + (totalObjects + 1) + " /Root 1 0 R >>\n");
        write(out, "startxref\n" + xrefOffset + "\n%%EOF\n");
        return out.toByteArray();
    }

    private void writeObject(ByteArrayOutputStream out, List<Integer> offsets, int number, String body)
            throws IOException {
        offsets.add(out.size());
        write(out, number + " 0 obj\n" + body + "\nendobj\n");
    }

    private void write(ByteArrayOutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static void addFooter(StringBuilder stream, int page, int total) {
        drawText(stream, PAGE_WIDTH / 2f - 28f, 22f, 9f,
                "Page " + page + " of " + total);
    }

    private static String valueOrBlank(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static String fit(String text, int maxChars) {
        String value = valueOrBlank(text);
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private static String escape(String text) {
        return valueOrBlank(text)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private static void drawText(StringBuilder stream, float x, float y, float size, String text) {
        stream.append("BT /F1 ").append(size).append(" Tf ")
                .append(x).append(' ').append(y).append(" Td (")
                .append(escape(text)).append(") Tj ET\n");
    }

    private static void drawLine(StringBuilder stream, float x1, float y1, float x2, float y2) {
        stream.append(x1).append(' ').append(y1).append(" m ")
                .append(x2).append(' ').append(y2).append(" l S\n");
    }

    private static void fillRect(StringBuilder stream, float x, float y, float width, float height, float gray) {
        stream.append(gray).append(" g ")
                .append(x).append(' ').append(y).append(' ')
                .append(width).append(' ').append(height).append(" re f 0 g\n");
    }

    private static final class PageCanvas {
        private final List<StringBuilder> pages;
        private StringBuilder stream;
        private float y;

        private PageCanvas(List<StringBuilder> pages) {
            this.pages = pages;
        }

        private void newPage() {
            stream = new StringBuilder();
            pages.add(stream);
            stream.append("0.8 w 0 G 0 g\n");
            y = PAGE_HEIGHT - TOP;
        }

        private boolean hasSpace(float height) {
            return y - height >= BOTTOM;
        }

        private void ensureSpace(float height) {
            if (!hasSpace(height)) {
                newPage();
            }
        }

        private void title(String text) {
            drawText(stream, LEFT, y, 18f, text);
            y -= 25f;
        }

        private void sectionHeading(String text) {
            ensureSpace(32f);
            fillRect(stream, LEFT, y - 20f, PAGE_WIDTH - LEFT - RIGHT, 25f, 0.90f);
            drawText(stream, LEFT + 8f, y - 13f, 13f, text);
            y -= 31f;
        }

        private void bodyText(String text) {
            drawText(stream, LEFT, y, 11f, text);
            y -= 17f;
        }

        private void smallText(String text) {
            drawText(stream, LEFT, y, 9.5f, text);
            y -= 15f;
        }

        private void gap(float amount) {
            y -= amount;
        }

        private void tableHeader() {
            ensureSpace(ROW_HEIGHT * 2f);
            float x = LEFT;
            fillRect(stream, x, y - ROW_HEIGHT, PAGE_WIDTH - LEFT - RIGHT, ROW_HEIGHT, 0.82f);
            for (int i = 0; i < HEADERS.length; i++) {
                drawText(stream, x + 5f, y - 15f, 9.5f, HEADERS[i]);
                drawLine(stream, x, y, x, y - ROW_HEIGHT);
                x += WIDTHS[i];
            }
            drawLine(stream, x, y, x, y - ROW_HEIGHT);
            drawLine(stream, LEFT, y, x, y);
            drawLine(stream, LEFT, y - ROW_HEIGHT, x, y - ROW_HEIGHT);
            y -= ROW_HEIGHT;
        }

        private void tableRow(String[] values) {
            ensureSpace(ROW_HEIGHT);
            float x = LEFT;
            int[] maxChars = {24, 26, 30, 14, 10};
            for (int i = 0; i < WIDTHS.length; i++) {
                String value = i < values.length ? fit(values[i], maxChars[i]) : "";
                float textX = (i == 4) ? x + WIDTHS[i] / 2f - 3f : x + 5f;
                drawText(stream, textX, y - 15f, 9.5f, value);
                drawLine(stream, x, y, x, y - ROW_HEIGHT);
                x += WIDTHS[i];
            }
            drawLine(stream, x, y, x, y - ROW_HEIGHT);
            drawLine(stream, LEFT, y - ROW_HEIGHT, x, y - ROW_HEIGHT);
            y -= ROW_HEIGHT;
        }
    }
}
