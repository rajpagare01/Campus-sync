package com.campussync.backend.Repository;

import com.campussync.backend.Model.Follow;
import com.campussync.backend.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // Check if user A follows user B
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // Find follow relationship between two users
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // Count followers of a user (people following this user)
    long countByFollowingId(Long followingId);

    // Count following of a user (people this user follows)
    long countByFollowerId(Long followerId);

    // Get all followers of a user (paginated)
    @Query("SELECT f FROM Follow f WHERE f.following.id = :userId ORDER BY f.createdAt DESC")
    Page<Follow> findFollowersByUserId(@Param("userId") Long userId, Pageable pageable);

    // Get all users that a user is following (paginated)
    @Query("SELECT f FROM Follow f WHERE f.follower.id = :userId ORDER BY f.createdAt DESC")
    Page<Follow> findFollowingByUserId(@Param("userId") Long userId, Pageable pageable);

    // Get followers as User entities (paginated)
    @Query("SELECT u FROM users u JOIN Follow f ON f.follower.id = u.id WHERE f.following.id = :userId ORDER BY f.createdAt DESC")
    Page<User> findFollowerUsersByUserId(@Param("userId") Long userId, Pageable pageable);

    // Get following as User entities (paginated)
    @Query("SELECT u FROM users u JOIN Follow f ON f.following.id = u.id WHERE f.follower.id = :userId ORDER BY f.createdAt DESC")
    Page<User> findFollowingUsersByUserId(@Param("userId") Long userId, Pageable pageable);

    // Get IDs of users that a user is following
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);

    // Delete follow relationship
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // Get mutual follows (users who follow each other)
    @Query("""
        SELECT DISTINCT u FROM users u
        WHERE EXISTS (
            SELECT 1 FROM Follow f1 WHERE f1.follower.id = :userId AND f1.following.id = u.id
        )
        AND EXISTS (
            SELECT 1 FROM Follow f2 WHERE f2.follower.id = u.id AND f2.following.id = :userId
        )
        """)
    List<User> findMutualFollows(@Param("userId") Long userId);

    // Get recommended users to follow (users followed by people you follow, excluding already followed)
    @Query("""
        SELECT u FROM users u
        WHERE u.id != :userId
        AND u.id NOT IN (
            SELECT f2.following.id FROM Follow f2 WHERE f2.follower.id = :userId
        )
        AND u.id IN (
            SELECT f3.following.id FROM Follow f3
            WHERE f3.follower.id IN (
                SELECT f4.following.id FROM Follow f4 WHERE f4.follower.id = :userId
            )
        )
        ORDER BY (
            SELECT COUNT(*) FROM Follow f5 WHERE f5.follower.id IN (
                SELECT f6.following.id FROM Follow f6 WHERE f6.follower.id = :userId
            ) AND f5.following.id = u.id
        ) DESC
        """)
    Page<User> findRecommendedUsersToFollow(@Param("userId") Long userId, Pageable pageable);
    // Get popular users to follow (most followers, excluding self and already followed)
    @Query("""
        SELECT u FROM users u
        WHERE u.id != :userId
        AND u.id NOT IN (
            SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId
        )
        ORDER BY (
            SELECT COUNT(f2) FROM Follow f2 WHERE f2.following.id = u.id
        ) DESC
        """)
    Page<User> findPopularUsersToFollow(@Param("userId") Long userId, Pageable pageable);
}
