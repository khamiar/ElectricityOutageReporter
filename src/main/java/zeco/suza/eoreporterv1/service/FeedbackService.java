package zeco.suza.eoreporterv1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zeco.suza.eoreporterv1.exception.ResourceNotFoundException;
import zeco.suza.eoreporterv1.model.Feedback;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.repository.FeedbackRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {
    
    @Autowired
    private FeedbackRepository feedbackRepository;
    
    public Feedback createFeedback(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }
    
    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllOrderByCreatedAtDesc();
    }
    
    public List<Feedback> getFeedbackByUser(Users user) {
        return feedbackRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public List<Feedback> getFeedbackByStatus(String status) {
        return feedbackRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findByIdWithUserData(id);
    }
    
    public Feedback updateFeedbackStatus(Long id, String status, String response, Users respondedBy) {
        Optional<Feedback> optionalFeedback = feedbackRepository.findById(id);
        if (optionalFeedback.isPresent()) {
            Feedback feedback = optionalFeedback.get();
            feedback.setStatus(status);
            if (response != null && !response.trim().isEmpty()) {
                feedback.setResponse(response);
                feedback.setRespondedBy(respondedBy);
                feedback.setRespondedAt(LocalDateTime.now());
            }
            feedbackRepository.save(feedback);
            
            // Fetch the updated feedback with user data
            return feedbackRepository.findByIdWithUserData(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with id: " + id));
        } else {
            throw new ResourceNotFoundException("Feedback not found with id: " + id);
        }
    }
    
    public void deleteFeedback(Long id) {
        if (feedbackRepository.existsById(id)) {
            feedbackRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Feedback not found with id: " + id);
        }
    }
    
    public long countFeedbackByStatus(String status) {
        return feedbackRepository.countByStatus(status);
    }
    
    public long getTotalFeedbackCount() {
        return feedbackRepository.count();
    }
} 