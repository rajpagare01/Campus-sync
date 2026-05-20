package com.campussync.backend.Repository;

import com.campussync.backend.Model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // Find posts by author
    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    // Find posts linked to specific event
    List<Post> findByLinkedEventIdOrderByCreatedAtDesc(Long eventId);

    // Find posts with media
    List<Post> findByMediaUrlIsNotNullOrderByCreatedAtDesc();

    // Count posts by author
    long countByAuthorId(Long authorId);

    // Find recent posts (for feed)
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findRecentPosts();

    // Find posts by author with pagination support
    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId ORDER BY p.createdAt DESC")
    List<Post> findByAuthorId(@Param("authorId") Long authorId);

    // 🆕 Pagination support for recent posts
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    Page<Post> findRecentPosts(Pageable pageable);

    // 🆕 Pagination support for posts by author
    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    // 🆕 Pagination support for posts with media
    Page<Post> findByMediaUrlIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    // 🆕 Pagination support for posts linked to event
    Page<Post> findByLinkedEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    // Analytics queries
    List<Post> findByCreatedAtAfter(LocalDateTime date);

    long countByCreatedAtAfter(LocalDateTime date);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByLinkedEvent_Id(Long eventId);

    // 🆕 Search method
    Page<Post> findByContentContainsIgnoreCase(String content, Pageable pageable);
}
