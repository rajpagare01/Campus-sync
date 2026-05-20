package com.campussync.backend.Repository;

import com.campussync.backend.Model.Comment;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Comment Repository Tests")
class CommentRepositorySimpleTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        author = new User();
        author.setName("Author");
        author.setEmail("author@test.com");
        author.setPassword("password");
        author.setRole(Role.STUDENT);
        author = userRepository.save(author);

        User postAuthor = new User();
        postAuthor.setName("Post Author");
        postAuthor.setEmail("postauthor@test.com");
        postAuthor.setPassword("password");
        postAuthor.setRole(Role.SOCIETY);
        postAuthor = userRepository.save(postAuthor);

        post = new Post();
        post.setContent("Test post");
        post.setAuthor(postAuthor);
        post.setCreatedAt(LocalDateTime.now());
        post = postRepository.save(post);
    }

    @Test
    @DisplayName("Should save comment")
    void testSaveComment() {
        Comment comment = new Comment();
        comment.setContent("Test comment");
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContent()).isEqualTo("Test comment");
    }

    @Test
    @DisplayName("Should count comments on post")
    void testCountByPostId() {
        Comment comment = new Comment();
        comment.setContent("Test comment");
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        long count = commentRepository.countByPostId(post.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find comments by post")
    void testFindByPostId() {
        Comment comment = new Comment();
        comment.setContent("Test comment");
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        List<Comment> comments = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(post.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getContent()).isEqualTo("Test comment");
    }
}
