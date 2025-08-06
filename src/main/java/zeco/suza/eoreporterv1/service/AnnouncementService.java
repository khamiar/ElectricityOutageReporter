package zeco.suza.eoreporterv1.service;

import zeco.suza.eoreporterv1.model.Announcement;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.repository.AnnouncementRepository;
import zeco.suza.eoreporterv1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zeco.suza.eoreporterv1.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class AnnouncementService {
    @Autowired
    private AnnouncementRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public List<Announcement> getAll() {
        return repository.findAll();
    }
    
    public Page<Announcement> getAllPaginated(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<Announcement> getById(Long id) {
        return repository.findById(id);
    }

    public Announcement create(Announcement announcement) {
        Announcement saved = repository.save(announcement);

        // NEW: Send notification to all users if sendNotification is true
        if (Boolean.TRUE.equals(saved.getSendNotification())) {
            List<Users> users = userRepository.findAll();
            for (Users user : users) {
                if (!user.getId().equals(saved.getPostedBy().getId())) { // skip admin
                    notificationService.createNotification(
                        user,
                        "New Announcement",
                        saved.getTitle(),
                        null
                    );
                }
            }
        }

        return saved;
    }


public Announcement update(Long id, Announcement announcement) {
    return repository.findById(id)
        .map(existing -> {
            // Validate that only the original poster or admin can update
            if (announcement.getPostedBy() != null && 
                !announcement.getPostedBy().getId().equals(existing.getPostedBy().getId())) {
                throw new SecurityException("Cannot change announcement ownership");
            }
            
            existing.setTitle(announcement.getTitle());
            existing.setContent(announcement.getContent());
            existing.setCategory(announcement.getCategory());
            existing.setAttachmentUrl(announcement.getAttachmentUrl());
            existing.setPublishDate(announcement.getPublishDate());
            existing.setSendNotification(announcement.getSendNotification());
            existing.setStatus(announcement.getStatus());
            // Don't update postedBy - keep original poster
            return repository.save(existing);
        })
        .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
}

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Announcement not found with id: " + id);
        }
        repository.deleteById(id);
    }

    public List<Announcement> getByCategory(String category) {
        return repository.findByCategory(category);
    }

    public List<Announcement> getByStatus(String status) {
        if (!List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(status)) {
            throw new IllegalArgumentException("Invalid status value. Must be one of: DRAFT, PUBLISHED, ARCHIVED");
        }
        return repository.findByStatus(status);
    }
}