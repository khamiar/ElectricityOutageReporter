package zeco.suza.eoreporterv1.controller;

import org.springframework.web.multipart.MultipartFile;
import zeco.suza.eoreporterv1.model.Announcement;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.service.AnnouncementService;
import zeco.suza.eoreporterv1.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    @Autowired
    private AnnouncementService service;

    @Autowired
    private FileStorageService storageService;

    @GetMapping
    public List<Announcement> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Announcement> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Announcement> create(
            @RequestBody Announcement announcement,
            @AuthenticationPrincipal Users currentUser) {
        if (currentUser != null) {
            System.out.println("Current user: " + currentUser.getEmail());
            System.out.println("User role: " + currentUser.getRole());
            System.out.println("User authorities: " + currentUser.getAuthorities());
            announcement.setPostedBy(currentUser);
        } else {
            System.out.println("No authenticated user found");
            // You might want to create a default user or handle this case differently
        }
        return ResponseEntity.ok(service.create(announcement));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Announcement> update(
            @PathVariable Long id, 
            @RequestBody Announcement announcement,
            @AuthenticationPrincipal Users currentUser) {
        return ResponseEntity.ok(service.update(id, announcement));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{category}")
    public List<Announcement> getByCategory(@PathVariable String category) {
        return service.getByCategory(category);
    }

    @GetMapping("/status/{status}")
    public List<Announcement> getByStatus(@PathVariable String status) {
        return service.getByStatus(status);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = storageService.store(file);
            Map<String, String> response = new HashMap<>();
            response.put("filename", filename);
            response.put("message", "File uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "File upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 