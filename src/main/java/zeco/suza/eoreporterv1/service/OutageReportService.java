// --- SERVICE ---
package zeco.suza.eoreporterv1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zeco.suza.eoreporterv1.exception.ResourceNotFoundException;
import zeco.suza.eoreporterv1.model.*;
import zeco.suza.eoreporterv1.repository.OutageReportRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.BaseColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OutageReportService {
    private final OutageReportRepository reportRepository;
    private final GeoLocationService geoLocationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Path rootLocation = Paths.get("upload-dir");

    public OutageReport createReport(OutageReport report, MultipartFile media, Users reporter) throws IOException {
        report.setReporter(reporter);
        report.setReportedAt(LocalDateTime.now());
        report.setStatus(OutageStatus.PENDING);

        // 🔁 REVERSE GEOCODING
        if (report.getLatitude() != null && report.getLongitude() != null) {
            String locationName = geoLocationService.reverseGeocode(report.getLatitude(), report.getLongitude());
            report.setLocationName(locationName);
        }

        if (media != null && !media.isEmpty()) {
            String filename = UUID.randomUUID().toString() + "_" + media.getOriginalFilename();
            Files.createDirectories(rootLocation);
            Files.copy(media.getInputStream(), rootLocation.resolve(filename));
            report.setMediaUrl("/uploads/" + filename);
        }

        OutageReport savedReport = reportRepository.save(report);
        
        // Send WebSocket message for new outage
        messagingTemplate.convertAndSend("/topic/outages", savedReport);
        
        return savedReport;
    }

    public List<OutageReport> getUserReports(Users reporter) {
        return reportRepository.findByReporter(reporter);
    }

    public OutageReport updateStatus(Long id, OutageStatus status) {
        OutageReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(status);
        if (status == OutageStatus.RESOLVED) {
            report.setResolvedAt(LocalDateTime.now());
        }
        
        OutageReport updatedReport = reportRepository.save(report);
        
        // Send WebSocket message for status update
        messagingTemplate.convertAndSend("/topic/outage-status", updatedReport);
        
        return updatedReport;
    }

    public Map<String, Long> getStatusSummary() {
        Map<String, Long> summary = new HashMap<>();
        summary.put("total", reportRepository.count());
        summary.put("pending", reportRepository.countByStatus(OutageStatus.PENDING));
        summary.put("in_progress", reportRepository.countByStatus(OutageStatus.IN_PROGRESS));
        summary.put("resolved", reportRepository.countByStatus(OutageStatus.RESOLVED));
        return summary;
    }

    public List<OutageReport> getAllReports() {
        return reportRepository.findAll();
    }
    
    public void deleteReport(Long id) {
        OutageReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        
        // Delete associated media file if exists
        if (report.getMediaUrl() != null) {
            try {
                String filename = report.getMediaUrl().replace("/uploads/", "");
                Files.deleteIfExists(rootLocation.resolve(filename));
            } catch (IOException e) {
                // Log error but continue with report deletion
                System.err.println("Error deleting media file: " + e.getMessage());
            }
        }
        
        reportRepository.delete(report);
        
        // Send WebSocket message for outage deletion
        messagingTemplate.convertAndSend("/topic/outage-deleted", report.getId());
    }

    public byte[] generateReport(String fromDate, String toDate, String format) {
        try {
            // Parse dates with explicit format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startLocalDate = LocalDate.parse(fromDate, formatter);
            LocalDate endLocalDate = LocalDate.parse(toDate, formatter);
            
            LocalDateTime startDate = startLocalDate.atStartOfDay();
            LocalDateTime endDate = endLocalDate.atTime(23, 59, 59);
            
            System.out.println("Searching for reports between: " + startDate + " and " + endDate);
            
            List<OutageReport> reports = reportRepository.findByReportedAtBetween(startDate, endDate);
            
            System.out.println("Found " + reports.size() + " reports");
            
            if (reports.isEmpty()) {
                throw new RuntimeException("No reports found for the selected date range");
            }
            
            if (format.equalsIgnoreCase("pdf")) {
                return generatePdfReport(reports);
            } else if (format.equalsIgnoreCase("excel")) {
                return generateExcelReport(reports);
            } else {
                throw new IllegalArgumentException("Unsupported format: " + format);
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD format", e);
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error generating report: " + e.getMessage(), e);
        }
    }

    private byte[] generatePdfReport(List<OutageReport> reports) {
        Document document = null;
        try {
            document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            
            document.open();
            
            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Outage Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Add date range
            Font dateFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            Paragraph dateRange = new Paragraph(
                String.format("Period: %s to %s", 
                    reports.get(0).getReportedAt().toLocalDate(),
                    reports.get(reports.size() - 1).getReportedAt().toLocalDate()),
                dateFont
            );
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);
            
            // Add table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            
            // Add headers
            String[] headers = {"Title", "Location", "Status", "Reported At", "Resolved At"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new BaseColor(240, 240, 240)); // Light gray color
                table.addCell(cell);
            }
            
            // Add data
            for (OutageReport report : reports) {
                table.addCell(report.getTitle());
                table.addCell(report.getLocationName());
                table.addCell(report.getStatus().toString());
                table.addCell(report.getReportedAt().toString());
                table.addCell(report.getResolvedAt() != null ? report.getResolvedAt().toString() : "N/A");
            }
            
            document.add(table);
            document.close();
            
            return out.toByteArray();
        } catch (Exception e) {
            if (document != null && document.isOpen()) {
                document.close();
            }
            throw new RuntimeException("Error generating PDF report: " + e.getMessage(), e);
        }
    }

    private byte[] generateExcelReport(List<OutageReport> reports) {
        Workbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Outage Report");
            
            // Create header row with style
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);


            String[] headers = {"Title", "Location", "Status", "Reported At", "Resolved At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Add data rows
            int rowNum = 1;
            for (OutageReport report : reports) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(report.getTitle());
                row.createCell(1).setCellValue(report.getLocationName());
                row.createCell(2).setCellValue(report.getStatus().toString());
                row.createCell(3).setCellValue(report.getReportedAt().toString());
                row.createCell(4).setCellValue(report.getResolvedAt() != null ? report.getResolvedAt().toString() : "N/A");
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel report: " + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException ex) {
                    // Ignore close error
                }
            }
        }
    }
}