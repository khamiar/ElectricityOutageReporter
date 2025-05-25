package zeco.suza.eoreporterv1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zeco.suza.eoreporterv1.model.Notification;
import zeco.suza.eoreporterv1.model.OutageReport;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.repository.NotificationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public Notification createNotification(Users recipient, String title, String message, OutageReport relatedReport) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .relatedReport(relatedReport)
                .build();
        
        return notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Users user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    public List<Notification> getUnreadNotifications(Users user) {
        return notificationRepository.findByRecipientAndIsReadFalse(user);
    }

    public long getUnreadNotificationCount(Users user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void markAllAsRead(Users user) {
        List<Notification> unreadNotifications = getUnreadNotifications(user);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
} 