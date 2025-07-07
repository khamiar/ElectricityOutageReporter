package zeco.suza.eoreporterv1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import zeco.suza.eoreporterv1.model.Feedback;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {
    private Long id;
    private String subject;
    private String message;
    private String status;
    private String response;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime respondedAt;
    
    private UserDTO user;
    private UserDTO respondedBy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {
        private Long id;
        private String fullName;
        private String email;
    }
    
    public static FeedbackDTO fromEntity(Feedback feedback) {
        UserDTO userDTO = null;
        if (feedback.getUser() != null) {
            userDTO = UserDTO.builder()
                    .id(feedback.getUser().getId())
                    .fullName(feedback.getUser().getFullName())
                    .email(feedback.getUser().getEmail())
                    .build();
        }
        
        UserDTO respondedByDTO = null;
        if (feedback.getRespondedBy() != null) {
            respondedByDTO = UserDTO.builder()
                    .id(feedback.getRespondedBy().getId())
                    .fullName(feedback.getRespondedBy().getFullName())
                    .email(feedback.getRespondedBy().getEmail())
                    .build();
        }
        
        return FeedbackDTO.builder()
                .id(feedback.getId())
                .subject(feedback.getSubject())
                .message(feedback.getMessage())
                .status(feedback.getStatus())
                .response(feedback.getResponse())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .respondedAt(feedback.getRespondedAt())
                .user(userDTO)
                .respondedBy(respondedByDTO)
                .build();
    }
} 