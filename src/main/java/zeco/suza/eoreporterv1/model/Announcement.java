package zeco.suza.eoreporterv1.model;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {
    // Getters and setters
    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    private String title;

    @Setter
    @Getter
    private String content;

    @Getter
    @Setter
    private String category;
    @Getter
    @Setter
    private String attachmentUrl;
    @Setter
    @Getter
    private LocalDateTime publishDate;
    @Setter
    @Getter
    private Boolean sendNotification;
    @Setter
    @Getter
    private String status;
    @Getter
    private LocalDateTime createdAt;
    @Getter
    private LocalDateTime updatedAt;
    
    @Setter
    @Getter
    private LocalDateTime postedAt;

    @ManyToOne
    @JoinColumn(name = "posted_by")
    private Users postedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        postedAt = LocalDateTime.now();
        if (status == null) {
            status = "DRAFT";
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }

}