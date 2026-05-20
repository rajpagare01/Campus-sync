package com.campussync.backend.Repository;

import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import com.campussync.backend.Model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PostRepository
 * Tests post queries and operations
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PostRepository Unit Tests")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;
    private Post testPost;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setName("Author");
        author.setEmail("author@example.com");
        author.setPassword("password123");
        author.setRole(Role.SOCIETY);
        author = userRepository.save(author);

        testPost = new Post();
        testPost.setContent("Test post content");
        testPost.setAuthor(author);
        testPost.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should save and retrieve post")
    void testSaveAndRetrievePost() {
        // When
        Post savedPost = postRepository.save(testPost);

        // Then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getContent()).isEqualTo("Test post content");
        assertThat(savedPost.getAuthor().getId()).isEqualTo(author.getId());
    }

    @Test
    @DisplayName("Should find post by ID")
    void testFindById() {
        // Given
        Post savedPost = postRepository.save(testPost);

        // When
        Optional<Post> foundPost = postRepository.findById(savedPost.getId());

        // Then
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getContent()).isEqualTo("Test post content");
    }

    @Test
    @DisplayName("Should return empty for non-existent post")
    void testFindByIdNotFound() {
        // When
        Optional<Post> foundPost = postRepository.findById(999L);

        // Then
        assertThat(foundPost).isEmpty();
    }

    @Test
    @DisplayName("Should count posts by author")
    void testCountByAuthorId() {
        // Given
        Post post1 = new Post();
        post1.setContent("Post 1");
        post1.setAuthor(author);
        post1.setCreatedAt(LocalDateTime.now());
        postRepository.save(post1);

        Post post2 = new Post();
        post2.setContent("Post 2");
        post2.setAuthor(author);
        post2.setCreatedAt(LocalDateTime.now());
        postRepository.save(post2);

        // When
        long count = postRepository.countByAuthorId(author.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find paginated posts by author")
    void testFindByAuthorId() {
        // Given
        Post post1 = new Post();
        post1.setContent("Post 1");
        post1.setAuthor(author);
        post1.setCreatedAt(LocalDateTime.now());
        postRepository.save(post1);

        // When
        Page<Post> posts = postRepository.findByAuthorId(
                author.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(posts.getContent()).hasSize(1);
        assertThat(posts.getContent().get(0).getContent()).isEqualTo("Post 1");
    }

    @Test
    @DisplayName("Should delete post")
    void testDeletePost() {
        // Given
        Post savedPost = postRepository.save(testPost);
        Long postId = savedPost.getId();

        // When
        postRepository.deleteById(postId);

        // Then
        Optional<Post> deletedPost = postRepository.findById(postId);
        assertThat(deletedPost).isEmpty();
    }

    @Test
    @DisplayName("Should update post")
    void testUpdatePost() {
        // Given
        Post savedPost = postRepository.save(testPost);

        // When
        savedPost.setContent("Updated content");
        Post updatedPost = postRepository.save(savedPost);

        // Then
        assertThat(updatedPost.getContent()).isEqualTo("Updated content");
    }
}
