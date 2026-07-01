package vn.vnpost.lunchorder.core.modules.feedback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vnpost.lunchorder.common.entity.Feedback;

import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsByUserIdAndMenuId(Long userId, Long menuId);
    Optional<Feedback> findByUserIdAndMenuId(Long userId, Long menuId);
}
