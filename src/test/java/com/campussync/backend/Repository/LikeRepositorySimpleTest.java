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

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Like Repository Tests")
class LikeRepositorySimpleTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        likeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setName("User");
        user.setEmail("user@test.com");
        user.setPassword("password");
        user.setRole(Role.STUDENT);
        user = userRepository.save(user);

        User author = new User();
        author.setName("Author");
        author.setEmail("author@test.com");
        author.setPassword("password");
        author.setRole(Role.SOCIETY);
        author = userRepository.save(author);

        post = new Post();
        post.setContent("Test post");
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());
        post = postRepository.save(post);
    }

    @Test
    @DisplayName("Should save like")
    void testSaveLike() {
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());

        Like saved = likeRepository.save(like);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Should check if like exists")
    void testExists() {
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());
        likeRepository.save(like);

        boolean exists = likeRepository.existsByUserIdAndPostId(user.getId(), post.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should count likes on post")
    void testCountByPostId() {
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());
        likeRepository.save(like);

        long count = likeRepository.countByPostId(post.getId());

        assertThat(count).isEqualTo(1);
    }
}
