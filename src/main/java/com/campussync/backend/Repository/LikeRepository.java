package com.campussync.backend.Repository;

import com.campussync.backend.Model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    @Query("SELECT l FROM Like l WHERE l.post.id = :postId ORDER BY l.createdAt DESC")
    List<Like> findByPostIdOrderByCreatedAtDesc(@Param("postId") Long postId);

    @Query("SELECT l FROM Like l WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    List<Like> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    void deleteByPostId(Long postId);

    // Bug #10 Fix: efficient count without fetching all records
    long countByUserId(Long userId);

    long countByCreatedAtAfter(LocalDateTime date);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.linkedEvent.id = :eventId")
    long countByPostEventId(@Param("eventId") Long eventId);
}
