package zeco.suza.eoreporterv1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OutageSocketDTO {
    private Long id;
    private String title;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String status;
    private LocalDateTime reportedAt;
    private String markerColor;
}
