package com.campussync.backend.Repository;

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

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Post Repository Tests")
class PostRepositorySimpleTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        postRepository.deleteAll();

        author = new User();
        author.setName("Author");
        author.setEmail("author@test.com");
        author.setPassword("password");
        author.setRole(Role.SOCIETY);
        author = userRepository.save(author);
    }

    @Test
    @DisplayName("Should save post")
    void testSavePost() {
        Post post = new Post();
        post.setContent("Test post");
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());

        Post saved = postRepository.save(post);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContent()).isEqualTo("Test post");
    }

    @Test
    @DisplayName("Should find post by ID")
    void testFindById() {
        Post post = new Post();
        post.setContent("Test post");
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());
        Post saved = postRepository.save(post);

        Optional<Post> found = postRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("Test post");
    }


    @Test
    @DisplayName("Should count posts by author")
    void testCountByAuthorId() {
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

        long count = postRepository.countByAuthorId(author.getId());

        assertThat(count).isEqualTo(2);
    }
}
