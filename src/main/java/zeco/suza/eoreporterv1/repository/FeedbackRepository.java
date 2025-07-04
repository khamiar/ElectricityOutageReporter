package zeco.suza.eoreporterv1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zeco.suza.eoreporterv1.model.Feedback;
import zeco.suza.eoreporterv1.model.Users;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    @Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.user LEFT JOIN FETCH f.respondedBy WHERE f.user = :user ORDER BY f.createdAt DESC")
    List<Feedback> findByUserOrderByCreatedAtDesc(@Param("user") Users user);
    
    @Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.user LEFT JOIN FETCH f.respondedBy WHERE f.status = :status ORDER BY f.createdAt DESC")
    List<Feedback> findByStatusOrderByCreatedAtDesc(@Param("status") String status);
    
    @Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.user LEFT JOIN FETCH f.respondedBy ORDER BY f.createdAt DESC")
    List<Feedback> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.user LEFT JOIN FETCH f.respondedBy WHERE f.id = :id")
    Optional<Feedback> findByIdWithUserData(@Param("id") Long id);
} 