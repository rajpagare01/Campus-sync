package com.campussync.backend.Repository;

import com.campussync.backend.Model.Follow;
import com.campussync.backend.Model.User;
import com.campussync.backend.Model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FollowRepository
 * Tests all follow relationship queries
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("FollowRepository Unit Tests")
class FollowRepositoryTest {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    private User follower;
    private User following;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        // Create test users
        follower = new User();
        follower.setName("Follower User");
        follower.setEmail("follower@example.com");
        follower.setPassword("password123");
        follower.setRole(Role.STUDENT);
        follower = userRepository.save(follower);

        following = new User();
        following.setName("Following User");
        following.setEmail("following@example.com");
        following.setPassword("password123");
        following.setRole(Role.SOCIETY);
        following = userRepository.save(following);

        anotherUser = new User();
        anotherUser.setName("Another User");
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword("password123");
        anotherUser.setRole(Role.STUDENT);
        anotherUser = userRepository.save(anotherUser);
    }

    @Test
    @DisplayName("Should create follow relationship")
    void testCreateFollowRelationship() {
        // Given
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());

        // When
        Follow savedFollow = followRepository.save(follow);

        // Then
        assertThat(savedFollow.getId()).isNotNull();
        assertThat(savedFollow.getFollower().getId()).isEqualTo(follower.getId());
        assertThat(savedFollow.getFollowing().getId()).isEqualTo(following.getId());
    }

    @Test
    @DisplayName("Should check if follow relationship exists")
    void testExistsByFollowerIdAndFollowingId() {
        // Given
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        // When
        boolean exists = followRepository.existsByFollowerIdAndFollowingId(
                follower.getId(), following.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false for non-existent follow relationship")
    void testExistsByFollowerIdAndFollowingIdNotFound() {
        // When
        boolean exists = followRepository.existsByFollowerIdAndFollowingId(
                follower.getId(), anotherUser.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should find follow relationship by follower and following")
    void testFindByFollowerIdAndFollowingId() {
        // Given
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        // When
        Optional<Follow> foundFollow = followRepository.findByFollowerIdAndFollowingId(
                follower.getId(), following.getId());

        // Then
        assertThat(foundFollow).isPresent();
        assertThat(foundFollow.get().getFollower().getId()).isEqualTo(follower.getId());
    }

    @Test
    @DisplayName("Should count followers of a user")
    void testCountByFollowingId() {
        // Given
        Follow follow1 = new Follow();
        follow1.setFollower(follower);
        follow1.setFollowing(following);
        follow1.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow1);

        Follow follow2 = new Follow();
        follow2.setFollower(anotherUser);
        follow2.setFollowing(following);
        follow2.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow2);

        // When
        long count = followRepository.countByFollowingId(following.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count following of a user")
    void testCountByFollowerId() {
        // Given
        Follow follow1 = new Follow();
        follow1.setFollower(follower);
        follow1.setFollowing(following);
        follow1.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow1);

        Follow follow2 = new Follow();
        follow2.setFollower(follower);
        follow2.setFollowing(anotherUser);
        follow2.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow2);

        // When
        long count = followRepository.countByFollowerId(follower.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should get paginated followers")
    void testFindFollowersByUserId() {
        // Given
        Follow follow1 = new Follow();
        follow1.setFollower(follower);
        follow1.setFollowing(following);
        follow1.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow1);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Follow> followers = followRepository.findFollowersByUserId(following.getId(), pageable);

        // Then
        assertThat(followers.getContent()).hasSize(1);
        assertThat(followers.getContent().get(0).getFollower().getId()).isEqualTo(follower.getId());
    }

    @Test
    @DisplayName("Should get paginated following")
    void testFindFollowingByUserId() {
        // Given
        Follow follow1 = new Follow();
        follow1.setFollower(follower);
        follow1.setFollowing(following);
        follow1.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow1);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Follow> following_users = followRepository.findFollowingByUserId(follower.getId(), pageable);

        // Then
        assertThat(following_users.getContent()).hasSize(1);
        assertThat(following_users.getContent().get(0).getFollowing().getId())
                .isEqualTo(following.getId());
    }

    @Test
    @DisplayName("Should get follower users as User entities")
    void testFindFollowerUsersByUserId() {
        // Given
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> followers = followRepository.findFollowerUsersByUserId(following.getId(), pageable);

        // Then
        assertThat(followers.getContent()).hasSize(1);
        assertThat(followers.getContent().get(0).getId()).isEqualTo(follower.getId());
    }

    @Test
    @DisplayName("Should get following users as User entities")
    void testFindFollowingUsersByUserId() {
        // Given
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> followingUsers = followRepository.findFollowingUsersByUserId(follower.getId(), pageable);

        // Then
        assertThat(followingUsers.getContent()).hasSize(1);
        assertThat(followingUsers.getContent().get(0).getId()).isEqualTo(following.getId());
    }

    @Test
    @DisplayName("Should delete follow relationship")
    void testDeleteByFollowerIdAndFollowingId() {
        // Given
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        // When
        followRepository.deleteByFollowerIdAndFollowingId(follower.getId(), following.getId());

        // Then
        boolean exists = followRepository.existsByFollowerIdAndFollowingId(
                follower.getId(), following.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should find mutual follows")
    void testFindMutualFollows() {
        // Given - Create mutual follow relationship
        Follow follow1 = new Follow();
        follow1.setFollower(follower);
        follow1.setFollowing(following);
        follow1.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow1);

        Follow follow2 = new Follow();
        follow2.setFollower(following);
        follow2.setFollowing(follower);
        follow2.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow2);

        // When
        List<User> mutualFollows = followRepository.findMutualFollows(follower.getId());

        // Then
        assertThat(mutualFollows).hasSize(1);
        assertThat(mutualFollows.get(0).getId()).isEqualTo(following.getId());
    }

    @Test
    @DisplayName("Should get recommended users to follow")
    void testFindRecommendedUsersToFollow() {
        // Given - Setup network: follower -> following -> anotherUser
        Follow follow1 = new Follow();
        follow1.setFollower(follower);
        follow1.setFollowing(following);
        follow1.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow1);

        Follow follow2 = new Follow();
        follow2.setFollower(following);
        follow2.setFollowing(anotherUser);
        follow2.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow2);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> recommended = followRepository.findRecommendedUsersToFollow(
                follower.getId(), pageable);

        // Then
        assertThat(recommended.getContent()).hasSize(1);
        assertThat(recommended.getContent().get(0).getId()).isEqualTo(anotherUser.getId());
    }
}
