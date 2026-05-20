package com.campussync.backend.Repository;

import com.campussync.backend.Model.Like;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import com.campussync.backend.Model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LikeRepository
 * Tests like/dislike operations
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("LikeRepository Unit Tests")
class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setRole(Role.STUDENT);
        user = userRepository.save(user);

        User author = new User();
        author.setName("Author");
        author.setEmail("author@example.com");
        author.setPassword("password123");
        author.setRole(Role.SOCIETY);
        author = userRepository.save(author);

        post = new Post();
        post.setContent("Test post");
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());
        post = postRepository.save(post);
    }

    @Test
    @DisplayName("Should create like successfully")
    void testCreateLike() {
        // Given
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());

        // When
        Like savedLike = likeRepository.save(like);

        // Then
        assertThat(savedLike.getId()).isNotNull();
        assertThat(savedLike.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedLike.getPost().getId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("Should check if user liked post")
    void testExistsByUserIdAndPostId() {
        // Given
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());
        likeRepository.save(like);

        // When
        boolean exists = likeRepository.existsByUserIdAndPostId(user.getId(), post.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false for non-existent like")
    void testExistsByUserIdAndPostIdNotFound() {
        // When
        boolean exists = likeRepository.existsByUserIdAndPostId(user.getId(), post.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should count likes on post")
    void testCountByPostId() {
        // Given
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());
        likeRepository.save(like);

        // When
        long count = likeRepository.countByPostId(post.getId());

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should delete like")
    void testDeleteByUserIdAndPostId() {
        // Given
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());
        likeRepository.save(like);

        // When
        likeRepository.deleteByUserIdAndPostId(user.getId(), post.getId());

        // Then
        boolean exists = likeRepository.existsByUserIdAndPostId(user.getId(), post.getId());
        assertThat(exists).isFalse();
    }
}
