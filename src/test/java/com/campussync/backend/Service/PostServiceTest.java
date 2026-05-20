package com.campussync.backend.Service;

import com.campussync.backend.Dto.PostRequest;
import com.campussync.backend.Dto.PostResponse;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private LikeService likeService;

    @Mock
    private CommentService commentService;

    @Mock
    private RealtimeService realtimeService;

    @Mock
    private SearchIndexService searchIndexService;

    @InjectMocks
    private PostService postService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerCanUpdatePost() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setName("Owner");
        owner.setRole(Role.STUDENT);

        Event event = new Event();
        event.setId(99L);
        event.setTitle("Linked event");

        Post post = new Post();
        post.setId(10L);
        post.setAuthor(owner);
        post.setContent("Old content");
        post.setMediaUrl("existing-image.jpg");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(eventRepository.findById(99L)).thenReturn(Optional.of(event));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.updatePost(
                10L,
                new PostRequest("Updated content", "https://cdn.example.com/post.png", 99L)
        );

        assertThat(response.getContent()).isEqualTo("Updated content");
        assertThat(response.getMediaUrl()).isEqualTo("https://cdn.example.com/post.png");
        assertThat(response.getEventId()).isEqualTo(99L);
        assertThat(response.getEventTitle()).isEqualTo("Linked event");
    }

    @Test
    void omittedMediaUrlDoesNotClearExistingPostMedia() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setName("Owner");
        owner.setRole(Role.STUDENT);

        Post post = new Post();
        post.setId(10L);
        post.setAuthor(owner);
        post.setContent("Old content");
        post.setMediaUrl("existing-image.jpg");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.updatePost(
                10L,
                new PostRequest("Updated content", null, null)
        );

        assertThat(response.getContent()).isEqualTo("Updated content");
        assertThat(response.getMediaUrl()).isEqualTo("existing-image.jpg");
    }

    @Test
    void nonOwnerCannotUpdatePost() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other@example.com", null)
        );

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setRole(Role.STUDENT);

        User other = new User();
        other.setId(2L);
        other.setEmail("other@example.com");
        other.setRole(Role.STUDENT);

        Post post = new Post();
        post.setId(10L);
        post.setAuthor(owner);

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> postService.updatePost(10L, new PostRequest("Blocked", null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }
}
