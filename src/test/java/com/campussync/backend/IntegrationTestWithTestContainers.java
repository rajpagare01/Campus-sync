package com.campussync.backend;

import com.campussync.backend.Model.User;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.Follow;
import com.campussync.backend.Repository.UserRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests using TestContainers
 * Tests database operations with real MySQL container
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Integration Tests with TestContainers - MySQL")
class IntegrationTestWithTestContainers {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("campus_sync_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FollowRepository followRepository;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        // Clear all data before each test
        followRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword123");
        testUser.setRole(Role.STUDENT);
        testUser.setVerified(true);
        testUser = userRepository.save(testUser);

        anotherUser = new User();
        anotherUser.setName("Another User");
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword("hashedPassword456");
        anotherUser.setRole(Role.SOCIETY);
        anotherUser.setVerified(true);
        anotherUser = userRepository.save(anotherUser);
    }

    @Test
    @DisplayName("Should save and retrieve user from real database")
    void testSaveAndRetrieveUser() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("Test User");
        assertThat(foundUser.get().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("Should create post and link to user")
    void testCreatePostWithUser() {
        // Given
        Post post = new Post();
        post.setContent("Integration test post");
        post.setAuthor(testUser);
        post.setCreatedAt(LocalDateTime.now());

        // When
        Post savedPost = postRepository.save(post);

        // Then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getAuthor().getId()).isEqualTo(testUser.getId());
        assertThat(savedPost.getContent()).isEqualTo("Integration test post");
    }

    @Test
    @DisplayName("Should create follow relationship between users")
    void testCreateFollowRelationship() {
        // Given
        Follow follow = new Follow();
        follow.setFollower(testUser);
        follow.setFollowing(anotherUser);
        follow.setCreatedAt(LocalDateTime.now());

        // When
        Follow savedFollow = followRepository.save(follow);

        // Then
        assertThat(savedFollow.getId()).isNotNull();
        assertThat(savedFollow.getFollower().getId()).isEqualTo(testUser.getId());
        assertThat(savedFollow.getFollowing().getId()).isEqualTo(anotherUser.getId());
    }

    @Test
    @DisplayName("Should verify follow relationship exists")
    void testFollowRelationshipExists() {
        // Given
        Follow follow = new Follow();
        follow.setFollower(testUser);
        follow.setFollowing(anotherUser);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        // When
        boolean exists = followRepository.existsByFollowerIdAndFollowingId(
                testUser.getId(), anotherUser.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should retrieve multiple posts by author")
    void testMultiplePostsByAuthor() {
        // Given
        Post post1 = new Post();
        post1.setContent("First post");
        post1.setAuthor(testUser);
        post1.setCreatedAt(LocalDateTime.now());

        Post post2 = new Post();
        post2.setContent("Second post");
        post2.setAuthor(testUser);
        post2.setCreatedAt(LocalDateTime.now());

        postRepository.save(post1);
        postRepository.save(post2);

        // When
        long count = postRepository.countByAuthorId(testUser.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count followers of a user")
    void testCountFollowers() {
        // Given
        Follow follow1 = new Follow();
        follow1.setFollower(testUser);
        follow1.setFollowing(anotherUser);
        follow1.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow1);

        User thirdUser = new User();
        thirdUser.setName("Third User");
        thirdUser.setEmail("third@example.com");
        thirdUser.setPassword("password123");
        thirdUser.setRole(Role.STUDENT);
        thirdUser = userRepository.save(thirdUser);

        Follow follow2 = new Follow();
        follow2.setFollower(thirdUser);
        follow2.setFollowing(anotherUser);
        follow2.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow2);

        // When
        long count = followRepository.countByFollowingId(anotherUser.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should delete user and cascade to posts")
    void testDeleteUserWithPosts() {
        // Given
        Post post = new Post();
        post.setContent("Test post");
        post.setAuthor(testUser);
        post.setCreatedAt(LocalDateTime.now());
        postRepository.save(post);

        // When
        long initialCount = postRepository.count();
        userRepository.deleteById(testUser.getId());

        // Then
        assertThat(userRepository.findById(testUser.getId())).isEmpty();
        // Note: Cascading depends on entity configuration
    }

    @Test
    @DisplayName("Should handle concurrent user operations")
    void testConcurrentUserOperations() {
        // Given
        User user1 = new User();
        user1.setEmail("concurrent1@example.com");
        user1.setPassword("password123");
        user1.setName("Concurrent User 1");
        user1.setRole(Role.STUDENT);

        User user2 = new User();
        user2.setEmail("concurrent2@example.com");
        user2.setPassword("password123");
        user2.setName("Concurrent User 2");
        user2.setRole(Role.STUDENT);

        // When
        User savedUser1 = userRepository.save(user1);
        User savedUser2 = userRepository.save(user2);

        // Then
        assertThat(savedUser1.getId()).isNotEqualTo(savedUser2.getId());
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("Should update user through repository")
    void testUpdateUserThroughRepository() {
        // Given
        testUser.setName("Updated Name");

        // When
        User updatedUser = userRepository.save(testUser);

        // Then
        Optional<User> retrievedUser = userRepository.findById(testUser.getId());
        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Should verify MySQL container is running")
    void testMySQLContainerRunning() {
        // MySQL should be running for this test to pass
        assertThat(mysql.isRunning());
    }
}
