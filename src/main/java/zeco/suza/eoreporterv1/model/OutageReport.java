// --- MODEL ---
package zeco.suza.eoreporterv1.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutageReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users reporter;

    private String title;
    private String description;
    private String manualLocation;
    private Double latitude;
    private Double longitude;
    private String locationName;

    private String mediaUrl; // Image or Video

    @Enumerated(EnumType.STRING)
    private OutageStatus status;

    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        reportedAt = LocalDateTime.now();
        status = OutageStatus.PENDING;
    }
}