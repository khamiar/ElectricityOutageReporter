// --- REPOSITORY ---
package zeco.suza.eoreporterv1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zeco.suza.eoreporterv1.model.OutageReport;
import zeco.suza.eoreporterv1.model.OutageStatus;
import zeco.suza.eoreporterv1.model.Users;

import java.time.LocalDateTime;
import java.util.List;

public interface OutageReportRepository extends JpaRepository<OutageReport, Long> {
    @Query("SELECT o FROM OutageReport o LEFT JOIN FETCH o.reporter WHERE o.reporter = :reporter ORDER BY o.reportedAt DESC")
    List<OutageReport> findByReporter(@Param("reporter") Users reporter);
    
    long countByStatus(OutageStatus status);
    long count();
    
    @Query("SELECT o FROM OutageReport o LEFT JOIN FETCH o.reporter WHERE o.reportedAt BETWEEN :startDate AND :endDate ORDER BY o.reportedAt DESC")
    List<OutageReport> findByReportedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM OutageReport o LEFT JOIN FETCH o.reporter ORDER BY o.reportedAt DESC")
    List<OutageReport> findAll();
}