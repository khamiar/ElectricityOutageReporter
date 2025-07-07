package zeco.suza.eoreporterv1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zeco.suza.eoreporterv1.dto.FeedbackDTO;
import zeco.suza.eoreporterv1.model.Feedback;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.service.FeedbackService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    
    @Autowired
    private FeedbackService feedbackService;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFeedback(
            @RequestBody Map<String, String> feedbackData,
            @AuthenticationPrincipal Users currentUser) {
        try {
            String subject = feedbackData.get("subject");
            String message = feedbackData.get("message");
            
            if (subject == null || subject.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Subject is required"));
            }
            
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message is required"));
            }
            
            Feedback feedback = Feedback.builder()
                    .subject(subject.trim())
                    .message(message.trim())
                    .user(currentUser)
                    .build();
            
            Feedback savedFeedback = feedbackService.createFeedback(feedback);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedFeedback.getId());
            response.put("subject", savedFeedback.getSubject());
            response.put("status", savedFeedback.getStatus());
            response.put("createdAt", savedFeedback.getCreatedAt());
            response.put("message", "Feedback submitted successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to submit feedback: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<FeedbackDTO>> getMyFeedback(@AuthenticationPrincipal Users currentUser) {
        try {
            List<Feedback> feedbackList = feedbackService.getFeedbackByUser(currentUser);
            List<FeedbackDTO> feedbackDTOs = feedbackList.stream()
                    .map(FeedbackDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(feedbackDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FeedbackDTO> getFeedbackById(
            @PathVariable Long id,
            @AuthenticationPrincipal Users currentUser) {
        try {
            return feedbackService.getFeedbackById(id)
                    .filter(feedback -> feedback.getUser().getId().equals(currentUser.getId()) || 
                            currentUser.getRole().toString().equals("ADMIN"))
                    .map(FeedbackDTO::fromEntity)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Admin endpoints
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FeedbackDTO>> getAllFeedback() {
        try {
            List<Feedback> feedbackList = feedbackService.getAllFeedback();
            List<FeedbackDTO> feedbackDTOs = feedbackList.stream()
                    .map(FeedbackDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(feedbackDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FeedbackDTO>> getFeedbackByStatus(@PathVariable String status) {
        try {
            List<Feedback> feedbackList = feedbackService.getFeedbackByStatus(status.toUpperCase());
            List<FeedbackDTO> feedbackDTOs = feedbackList.stream()
                    .map(FeedbackDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(feedbackDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeedbackDTO> respondToFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, String> responseData,
            @AuthenticationPrincipal Users currentUser) {
        try {
            String status = responseData.get("status");
            String response = responseData.get("response");
            
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            Feedback updatedFeedback = feedbackService.updateFeedbackStatus(
                    id, status.toUpperCase(), response, currentUser);
            
            return ResponseEntity.ok(FeedbackDTO.fromEntity(updatedFeedback));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteFeedback(@PathVariable Long id) {
        try {
            feedbackService.deleteFeedback(id);
            return ResponseEntity.ok(Map.of("message", "Feedback deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete feedback: " + e.getMessage()));
        }
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getFeedbackStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", feedbackService.getTotalFeedbackCount());
            stats.put("pending", feedbackService.countFeedbackByStatus("PENDING"));
            stats.put("reviewed", feedbackService.countFeedbackByStatus("REVIEWED"));
            stats.put("resolved", feedbackService.countFeedbackByStatus("RESOLVED"));
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 