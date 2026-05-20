package com.campussync.backend.Repository;

import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    long countByUpdatedAtAfter(LocalDateTime date);

    long countByCreatedAtAfter(LocalDateTime date);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByUpdatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByCreatedAtBeforeAndUpdatedAtAfter(LocalDateTime before, LocalDateTime after);

    long countByCreatedAtBetweenAndUpdatedAtAfter(LocalDateTime start, LocalDateTime end, LocalDateTime updatedAfter);

    @Query("SELECT u FROM users u WHERE " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.bio) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    @Query("SELECT u FROM users u WHERE u.id IN (SELECT f.following.id FROM Follow f) ORDER BY (SELECT COUNT(f2) FROM Follow f2 WHERE f2.following.id = u.id) DESC")
    Page<User> findTopUsersByFollowers(Pageable pageable);

    Page<User> findByIsVerifiedTrue(Pageable pageable);

    @Query("""
            SELECT u FROM users u
            WHERE (:query IS NULL
                OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:active IS NULL OR u.active = :active)
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchForAdmin(@Param("query") String query,
                              @Param("role") Role role,
                              @Param("active") Boolean active,
                              Pageable pageable);
}
