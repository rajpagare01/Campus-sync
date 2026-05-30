package com.campussync.backend.Repository;

import com.campussync.backend.Model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(Long postId);

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    long countByPostId(Long postId);

    long countByParentCommentId(Long parentCommentId);

    long countByAuthorId(Long authorId);

    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<Object[]> countByPostIdIn(@Param("postIds") List<Long> postIds);

    boolean existsByPostIdAndAuthorId(Long postId, Long authorId);

    @Query("SELECT c FROM Comment c WHERE c.id = :commentId OR c.parentComment.id = :commentId OR c.parentComment.parentComment.id = :commentId ORDER BY c.createdAt ASC")
    List<Comment> findCommentThread(@Param("commentId") Long commentId);

    @Query("SELECT c FROM Comment c ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments();

    void deleteByParentCommentId(Long parentCommentId);

    void deleteByPostId(Long postId);

    long countByCreatedAtAfter(LocalDateTime date);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.linkedEvent.id = :eventId")
    long countByPostEventId(@Param("eventId") Long eventId);

    Page<Comment> findByContentContainsIgnoreCase(String content, Pageable pageable);

    Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);
}
