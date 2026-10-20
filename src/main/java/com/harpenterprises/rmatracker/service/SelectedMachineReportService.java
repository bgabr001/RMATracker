package com.harpenterprises.rmatracker.service;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;

/** Reports are generated from a snapshot of the selected rows, without database changes. */
public final class SelectedMachineReportService {
    private SelectedMachineReportService() {}
    public static final String[] HEADERS = {"RMA #", "County", "Serial #", "Machine Type", "Version", "Received"};
    public static final class Machine {
        public final String rma, county, serial, type, version;
        public final boolean received;
        public Machine(String rma, String county, String serial, String type, String version, boolean received) {
            this.rma = clean(rma); this.county = clean(county); this.serial = clean(serial);
            this.type = clean(type); this.version = clean(version); this.received = received;
        }
        public String[] cells() { return new String[]{rma,county,serial,type,version,received?"Yes":"No"}; }
    }
    private static String clean(String s) { return s == null ? "" : s; }
    public static String html(List<Machine> machines) {
        StringBuilder out = new StringBuilder("<html><body style='font-family:sans-serif'><h2>Selected Machines</h2><p>Total: ")
            .append(machines.size()).append("</p><table border='1' cellpadding='5' cellspacing='0'><tr>");
        for (String h : HEADERS) out.append("<th>").append(escape(h)).append("</th>");
        out.append("</tr>");
        for (Machine m : machines) { out.append("<tr>"); for (String c : m.cells()) out.append("<td>").append(escape(c)).append("</td>"); out.append("</tr>"); }
        return out.append("</table></body></html>").toString();
    }
    private static String escape(String s) { return clean(s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
    public static void excel(List<Machine> machines, Path target) throws IOException {
        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("Selected Machines");
            CellStyle headerStyle = book.createCellStyle();
            Font font = book.createFont(); font.setBold(true); headerStyle.setFont(font);
            Row header = sheet.createRow(0);
            for (int i=0;i<HEADERS.length;i++) { Cell c=header.createCell(i); c.setCellValue(HEADERS[i]); c.setCellStyle(headerStyle); }
            int r=1;
            for (Machine m:machines) { Row row=sheet.createRow(r++); String[] cells=m.cells(); for(int i=0;i<cells.length;i++)row.createCell(i).setCellValue(cells[i]); }
            sheet.createFreezePane(0,1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,machines.size(),0,HEADERS.length-1));
            for(int i=0;i<HEADERS.length;i++) sheet.setColumnWidth(i, i==3?5500:4700);
            try(OutputStream out=java.nio.file.Files.newOutputStream(target)){book.write(out);}
        }
    }
    private static String pdfText(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : clean(s).toCharArray()) b.append(c >= 32 && c <= 126 ? c : '?');
        return b.toString();
    }
    public static void pdf(List<Machine> machines, Path target) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page=null; PDPageContentStream stream=null;
            float y=0;
            List<String> lines=new ArrayList<>();
            lines.add("SELECTED MACHINES  ("+machines.size()+")");
            lines.add(String.join(" | ",HEADERS));
            for(Machine m:machines) lines.add(String.join(" | ",m.cells()));
            for(String line:lines) {
                if(stream==null || y<48) {
                    if(stream!=null) stream.close();
                    page=new PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
                    doc.addPage(page); stream=new PDPageContentStream(doc,page); y=page.getMediaBox().getHeight()-48;
                }
                String value=pdfText(line);
                // Wrap at word boundaries to keep all columns visible on portrait pages.
                while(!value.isEmpty()) {
                    int end=Math.min(105,value.length());
                    if(end<value.length()) { int space=value.lastIndexOf(' ',end); if(space>40) end=space; }
                    stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER),8);
                    stream.newLineAtOffset(35,y);stream.showText(value.substring(0,end));stream.endText();y-=13;
                    value=value.substring(end).trim();
                    if(y<48 && !value.isEmpty()) {stream.close();page=new PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=page.getMediaBox().getHeight()-48;}
                }
            }
            if(stream!=null)stream.close();
            doc.save(target.toFile());
        }
    }
}
