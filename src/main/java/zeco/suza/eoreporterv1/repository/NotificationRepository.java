package zeco.suza.eoreporterv1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zeco.suza.eoreporterv1.model.Notification;
import zeco.suza.eoreporterv1.model.Users;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(Users recipient);
    List<Notification> findByRecipientAndIsReadFalse(Users recipient);
    long countByRecipientAndIsReadFalse(Users recipient);
} 