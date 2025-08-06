// --- MODEL ---
package zeco.suza.eoreporterv1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OutageReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private Users reporter;

    private String title;
    private String description;
    private String region;
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