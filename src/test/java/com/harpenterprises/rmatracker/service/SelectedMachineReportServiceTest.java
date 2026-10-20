package com.harpenterprises.rmatracker.service;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.Loader;
import static org.junit.jupiter.api.Assertions.*;

class SelectedMachineReportServiceTest {
    private List<SelectedMachineReportService.Machine> sample() {
        return List.of(new SelectedMachineReportService.Machine("RMA-1","Fayette","SC-123","Scanner","1.0",true));
    }
    @Test void previewEscapesContent() {
        String html=SelectedMachineReportService.html(List.of(new SelectedMachineReportService.Machine("<RMA>","A&B","1","Scanner","v",false)));
        assertTrue(html.contains("&lt;RMA&gt;")); assertTrue(html.contains("A&amp;B"));
    }
    @Test void excelContainsSelectedMachines() throws Exception {
        Path path=Files.createTempFile("machines", ".xlsx");
        try {SelectedMachineReportService.excel(sample(),path);
            try(var book=new XSSFWorkbook(Files.newInputStream(path))){
                assertEquals(2,book.getSheetAt(0).getPhysicalNumberOfRows());
                assertEquals("SC-123",book.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
            }
        } finally {Files.deleteIfExists(path);}
    }
    @Test void pdfContainsPage() throws Exception {
        Path path=Files.createTempFile("machines", ".pdf");
        try {SelectedMachineReportService.pdf(sample(),path);
            try(var doc=Loader.loadPDF(path.toFile())){assertTrue(doc.getNumberOfPages()>0);}
        } finally {Files.deleteIfExists(path);}
    }
}
