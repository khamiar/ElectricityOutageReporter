// --- CONTROLLER ---
package zeco.suza.eoreporterv1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import zeco.suza.eoreporterv1.model.*;
import zeco.suza.eoreporterv1.service.OutageReportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outages")
@RequiredArgsConstructor
public class OutageReportController {
    private final OutageReportService service;



    @PostMapping
    public ResponseEntity<OutageReport> createReport(
            @RequestPart("report") OutageReport report,
            @RequestPart(value = "media", required = false) MultipartFile media,
            @AuthenticationPrincipal Users reporter) throws Exception {
        return ResponseEntity.ok(service.createReport(report, media, reporter));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OutageReport> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String statusString = body.get("status");
        OutageStatus status = OutageStatus.valueOf(statusString.toUpperCase());
        return ResponseEntity.ok(service.updateStatus(id, status));
    }


    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getSummary() {
        return ResponseEntity.ok(service.getStatusSummary());
    }

    @GetMapping("/my")
    public ResponseEntity<List<OutageReport>> getMyReports(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(service.getUserReports(user));
    }

    @GetMapping
    public ResponseEntity<List<OutageReport>> getAllReports() {
        return ResponseEntity.ok(service.getAllReports());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        service.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateReport(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam String format) {
        try {
            byte[] reportData = service.generateReport(fromDate, toDate, format);
            String contentType = format.equalsIgnoreCase("pdf") ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            String filename = "outage_report_" + fromDate + "_to_" + toDate + "." + format.toLowerCase();
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(reportData);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
