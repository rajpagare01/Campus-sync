package com.campussync.backend.Repository;

import com.campussync.backend.Model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    Optional<Feedback> findByEventIdAndUserId(Long eventId, Long userId);
}
