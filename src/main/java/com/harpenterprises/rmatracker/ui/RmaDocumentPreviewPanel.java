package com.harpenterprises.rmatracker.ui;

import com.harpenterprises.rmatracker.model.RmaAttachment;
import com.harpenterprises.rmatracker.service.RmaAttachmentStorageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CancellationException;

/** Read-only in-application preview. Loads document contents outside the Swing event thread. */
public final class RmaDocumentPreviewPanel extends JPanel {
    private final RmaAttachmentStorageService storage;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel("Select an attachment to preview");
    private final JLabel pageLabel = new JLabel(" ");
    private final JButton previous = new JButton("Previous");
    private final JButton next = new JButton("Next");
    private final JComboBox<String> zoom = new JComboBox<>(new String[]{"50%", "75%", "100%", "125%", "150%"});
    private SwingWorker<PreviewData, Void> worker;
    private RmaAttachment selected;
    private Path selectedPath;
    private int pageIndex;
    private int pageCount;
    private long generation;

    public RmaDocumentPreviewPanel(RmaAttachmentStorageService storage) {
        super(new BorderLayout(6, 6));
        this.storage = storage;
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        bar.add(title);
        bar.add(previous);
        bar.add(next);
        bar.add(pageLabel);
        bar.add(new JLabel("Zoom:"));
        bar.add(zoom);
        zoom.setSelectedItem("100%");
        previous.addActionListener(e -> showPage(pageIndex - 1));
        next.addActionListener(e -> showPage(pageIndex + 1));
        zoom.addActionListener(e -> { if (selected != null && selectedPath != null && isPdf(selectedPath)) showPage(pageIndex); });
        add(bar, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        setMessage("Select a document from the list.");
        updateNavigation();
    }

    public void preview(RmaAttachment attachment) {
        generation++;
        if (worker != null) worker.cancel(true);
        selected = attachment;
        selectedPath = null;
        pageIndex = 0;
        pageCount = 0;
        title.setText(attachment == null ? "No document selected" : attachment.getOriginalFileName());
        updateNavigation();
        if (attachment == null) { setMessage("Select a document from the list."); return; }
        setMessage("Loading preview...");
        load(attachment, 0);
    }

    private void showPage(int target) {
        if (selected == null || selectedPath == null || target < 0 || target >= pageCount) return;
        if (worker != null) worker.cancel(true);
        pageIndex = target;
        updateNavigation();
        setMessage("Rendering page...");
        load(selected, target);
    }

    private void load(RmaAttachment attachment, int page) {
        final long request = ++generation;
        final int percent = Integer.parseInt(((String) zoom.getSelectedItem()).replace("%", ""));
        worker = new SwingWorker<>() {
            @Override protected PreviewData doInBackground() throws Exception {
                Path path = storage.getFile(attachment.getId());
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (isPdf(path)) {
                    try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
                        int count = pdf.getNumberOfPages();
                        if (count == 0) return PreviewData.message(path, "PDF contains no pages.");
                        if (page >= count) return PreviewData.message(path, "Page is unavailable.");
                        float dpi = 96f * percent / 100f;
                        BufferedImage image = new PDFRenderer(pdf).renderImageWithDPI(page, dpi);
                        return PreviewData.image(path, image, count);
                    }
                }
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                        name.endsWith(".gif") || name.endsWith(".bmp")) {
                    BufferedImage image = ImageIO.read(path.toFile());
                    if (image == null) throw new IOException("Unsupported or invalid image.");
                    return PreviewData.image(path, image, 0);
                }
                if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                    try (InputStream in = Files.newInputStream(path); Workbook wb = WorkbookFactory.create(in)) {
                        DefaultTableModel table = new DefaultTableModel();
                        int sheetCount = Math.min(wb.getNumberOfSheets(), 10);
                        DataFormatter formatter = new DataFormatter();
                        // Show the first worksheet in a read-only table; don't evaluate formulas or execute macros.
                        if (sheetCount == 0) return PreviewData.message(path, "Workbook contains no sheets.");
                        Sheet sheet = wb.getSheetAt(0);
                        int rows = Math.min(sheet.getLastRowNum() + 1, 2000);
                        int columns = 0;
                        for (int r = 0; r < rows; r++) {
                            Row row = sheet.getRow(r);
                            if (row != null) columns = Math.max(columns, row.getLastCellNum());
                        }
                        columns = Math.min(Math.max(columns, 1), 80);
                        for (int c = 0; c < columns; c++) table.addColumn(columnName(c));
                        for (int r = 0; r < rows; r++) {
                            Object[] values = new Object[columns];
                            Row row = sheet.getRow(r);
                            for (int c = 0; c < columns; c++) {
                                Cell cell = row == null ? null : row.getCell(c);
                                values[c] = cell == null ? "" : formatter.formatCellValue(cell);
                            }
                            table.addRow(values);
                        }
                        return PreviewData.table(path, table, "Sheet: " + sheet.getSheetName() + " (first 2,000 rows / 80 columns max)");
                    }
                }
                if (name.endsWith(".docx")) {
                    try (InputStream in = Files.newInputStream(path); XWPFDocument doc = new XWPFDocument(in)) {
                        StringBuilder sb = new StringBuilder();
                        for (XWPFParagraph p : doc.getParagraphs()) {
                            sb.append(p.getText()).append('\n');
                            if (sb.length() > 1_000_000) break;
                        }
                        return PreviewData.text(path, sb.toString(), "Word text preview (layout may differ)");
                    }
                }
                if (name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".log") || name.endsWith(".md")) {
                    if (Files.size(path) > 5_000_000) return PreviewData.message(path, "Text file too large to preview (5 MB limit).");
                    return PreviewData.text(path, Files.readString(path, StandardCharsets.UTF_8), "Text preview");
                }
                return PreviewData.message(path, "Preview is not available for this file type. Use Open to view it externally.");
            }
            @Override protected void done() {
                if (request != generation || isCancelled()) return;
                try {
                    PreviewData data = get();
                    selectedPath = data.path;
                    pageCount = data.pageCount;
                    pageIndex = page;
                    updateNavigation();
                    JPanel panel = new JPanel(new BorderLayout());
                    if (data.image != null) {
                        JLabel imageLabel = new JLabel(new ImageIcon(data.image));
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        panel.add(new JScrollPane(imageLabel), BorderLayout.CENTER);
                    } else if (data.table != null) {
                        JTable grid = new JTable(data.table);
                        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                        grid.setEnabled(false);
                        panel.add(new JScrollPane(grid), BorderLayout.CENTER);
                    } else if (data.text != null) {
                        JTextArea area = new JTextArea(data.text);
                        area.setEditable(false);
                        area.setLineWrap(true);
                        area.setWrapStyleWord(true);
                        area.setCaretPosition(0);
                        panel.add(new JScrollPane(area), BorderLayout.CENTER);
                    } else {
                        panel.add(new JLabel(data.note, SwingConstants.CENTER), BorderLayout.CENTER);
                    }
                    if (data.note != null && (data.image != null || data.table != null || data.text != null))
                        panel.add(new JLabel(data.note), BorderLayout.SOUTH);
                    setContent(panel);
                } catch (CancellationException ignored) {
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    setMessage("Preview unavailable: " + cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private static String columnName(int c) {
        StringBuilder s = new StringBuilder();
        for (int x = c + 1; x > 0; x = (x - 1) / 26) s.insert(0, (char)('A' + (x - 1) % 26));
        return s.toString();
    }
    private static boolean isPdf(Path path) { return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"); }
    private void updateNavigation() {
        boolean pdf = selectedPath != null && isPdf(selectedPath) && pageCount > 0;
        previous.setEnabled(pdf && pageIndex > 0);
        next.setEnabled(pdf && pageIndex + 1 < pageCount);
        zoom.setEnabled(selected != null);
        pageLabel.setText(pdf ? "Page " + (pageIndex + 1) + " of " + pageCount : " ");
    }
    private void setMessage(String text) { setContent(new JLabel(text, SwingConstants.CENTER)); }
    private void setContent(Component component) {
        content.removeAll(); content.add(component, BorderLayout.CENTER);
        content.revalidate(); content.repaint();
    }
    private static final class PreviewData {
        final Path path; final BufferedImage image; final DefaultTableModel table;
        final String text, note; final int pageCount;
        private PreviewData(Path p, BufferedImage i, DefaultTableModel t, String x, String n, int c) {
            path = p; image = i; table = t; text = x; note = n; pageCount = c;
        }
        static PreviewData image(Path p, BufferedImage i, int c) { return new PreviewData(p, i, null, null, null, c); }
        static PreviewData table(Path p, DefaultTableModel t, String n) { return new PreviewData(p, null, t, null, n, 0); }
        static PreviewData text(Path p, String s, String n) { return new PreviewData(p, null, null, s, n, 0); }
        static PreviewData message(Path p, String n) { return new PreviewData(p, null, null, null, n, 0); }
    }
}
