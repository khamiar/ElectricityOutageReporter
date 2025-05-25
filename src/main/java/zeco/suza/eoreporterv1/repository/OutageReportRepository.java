// --- REPOSITORY ---
package zeco.suza.eoreporterv1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zeco.suza.eoreporterv1.model.OutageReport;
import zeco.suza.eoreporterv1.model.OutageStatus;
import zeco.suza.eoreporterv1.model.Users;

import java.time.LocalDateTime;
import java.util.List;

public interface OutageReportRepository extends JpaRepository<OutageReport, Long> {
    List<OutageReport> findByReporter(Users reporter);
    long countByStatus(OutageStatus status);
    long count();
    List<OutageReport> findByReportedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}