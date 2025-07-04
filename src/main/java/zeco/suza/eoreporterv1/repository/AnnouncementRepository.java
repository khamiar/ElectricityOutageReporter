package zeco.suza.eoreporterv1.repository;

import zeco.suza.eoreporterv1.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByCategory(String category);
    List<Announcement> findByStatus(String status);
} 