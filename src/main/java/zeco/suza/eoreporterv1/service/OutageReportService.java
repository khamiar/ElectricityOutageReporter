package zeco.suza.eoreporterv1.service;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zeco.suza.eoreporterv1.dto.OutageSocketDTO;
import zeco.suza.eoreporterv1.exception.ResourceNotFoundException;
import zeco.suza.eoreporterv1.model.*;
import zeco.suza.eoreporterv1.repository.OutageReportRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OutageReportService {
    private final OutageReportRepository reportRepository;
    private final GeoLocationService geoLocationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final Path rootLocation = Paths.get("upload-dir");

    public OutageReport createReport(OutageReport report, MultipartFile media, Users reporter) throws IOException {
        report.setReporter(reporter);
        report.setReportedAt(LocalDateTime.now());
        report.setStatus(OutageStatus.PENDING);

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

        // 🔔 Push WebSocket DTO
        messagingTemplate.convertAndSend("/topic/outages", mapToSocketDTO(savedReport));

        return savedReport;
    }

    public OutageReport updateStatus(Long id, OutageStatus status) {
        OutageReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(status);
        if (status == OutageStatus.RESOLVED) {
            report.setResolvedAt(LocalDateTime.now());
        }

        OutageReport updated = reportRepository.save(report);

        // 🔔 Status-only payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", updated.getId());
        payload.put("status", updated.getStatus().toString());
        payload.put("resolvedAt", updated.getResolvedAt());

        messagingTemplate.convertAndSend("/topic/outage-status", payload);

         // NEW: Create notification for the reporter
        notificationService.createNotification(
            updated.getReporter(),
            "Outage Status Updated",
            "Your outage report #" + updated.getId() + " status changed to " + updated.getStatus(),
            updated
        );

        return updated;
    }

    public List<OutageReport> getUserReports(Users reporter) {
        return reportRepository.findByReporter(reporter);
    }

    public List<OutageReport> getAllReports() {
        return reportRepository.findAll();
    }

    public Map<String, Long> getStatusSummary() {
        Map<String, Long> summary = new HashMap<>();
        summary.put("total", reportRepository.count());
        summary.put("pending", reportRepository.countByStatus(OutageStatus.PENDING));
        summary.put("in_progress", reportRepository.countByStatus(OutageStatus.IN_PROGRESS));
        summary.put("resolved", reportRepository.countByStatus(OutageStatus.RESOLVED));
        return summary;
    }

    public void deleteReport(Long id) {
        OutageReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getMediaUrl() != null) {
            try {
                String filename = report.getMediaUrl().replace("/uploads/", "");
                Files.deleteIfExists(rootLocation.resolve(filename));
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + e.getMessage());
            }
        }

        reportRepository.delete(report);
        messagingTemplate.convertAndSend("/topic/outage-deleted", report.getId());
    }

    public byte[] generateReport(String fromDate, String toDate, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime startDate = LocalDate.parse(fromDate, formatter).atStartOfDay();
            LocalDateTime endDate = LocalDate.parse(toDate, formatter).atTime(23, 59, 59);

            List<OutageReport> reports = reportRepository.findByReportedAtBetween(startDate, endDate);

            if (reports.isEmpty()) throw new RuntimeException("No reports found");

            return switch (format.toLowerCase()) {
                case "pdf" -> generatePdfReport(reports);
                case "excel" -> generateExcelReport(reports);
                default -> throw new IllegalArgumentException("Invalid format: " + format);
            };

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Use format YYYY-MM-DD", e);
        }
    }

    private OutageSocketDTO mapToSocketDTO(OutageReport report) {
        return new OutageSocketDTO(
                report.getId(),
                report.getTitle(),
                report.getLatitude(),
                report.getLongitude(),
                report.getLocationName(),
                report.getStatus().toString(),
                report.getReportedAt(),
                getMarkerColor(report.getStatus())
        );
    }

    private String getMarkerColor(OutageStatus status) {
        return switch (status) {
            case PENDING -> "red";
            case IN_PROGRESS -> "orange";
            case RESOLVED -> "green";
        };
    }

    private byte[] generatePdfReport(List<OutageReport> reports) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Outage Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Paragraph range = new Paragraph(String.format("Period: %s to %s",
                    reports.get(0).getReportedAt().toLocalDate(),
                    reports.get(reports.size() - 1).getReportedAt().toLocalDate()));
            range.setAlignment(Element.ALIGN_CENTER);
            range.setSpacingAfter(20);
            document.add(range);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            Stream.of("Title", "Location", "Status", "Reported At", "Resolved At")
                    .forEach(col -> {
                        PdfPCell cell = new PdfPCell(new Phrase(col));
                        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        table.addCell(cell);
                    });

            for (OutageReport r : reports) {
                table.addCell(r.getTitle());
                table.addCell(r.getLocationName());
                table.addCell(r.getStatus().toString());
                table.addCell(r.getReportedAt().toString());
                table.addCell(r.getResolvedAt() != null ? r.getResolvedAt().toString() : "N/A");
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private byte[] generateExcelReport(List<OutageReport> reports) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Outage Report");

            Row headerRow = sheet.createRow(0);
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);

            String[] headers = {"Title", "Location", "Status", "Reported At", "Resolved At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(style);
            }

            int rowNum = 1;
            for (OutageReport r : reports) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getTitle());
                row.createCell(1).setCellValue(r.getLocationName());
                row.createCell(2).setCellValue(r.getStatus().toString());
                row.createCell(3).setCellValue(r.getReportedAt().toString());
                row.createCell(4).setCellValue(r.getResolvedAt() != null ? r.getResolvedAt().toString() : "N/A");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Excel generation failed", e);
        }
    }
}
