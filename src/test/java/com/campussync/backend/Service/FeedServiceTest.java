package com.campussync.backend.Service;

import com.campussync.backend.Dto.FeedItem;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CommentRepository commentRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RegistrationRepository registrationRepository;
    
    @Mock
    private FollowService followService;

    @InjectMocks
    private FeedService feedService;

    @Test
    void homeFeedUsesRealPostCommentCounts() {
        User author = new User();
        author.setId(10L);
        author.setName("Student User");

        Post post = new Post();
        post.setId(1L);
        post.setAuthor(author);
        post.setContent("Campus update");
        post.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        when(postRepository.findRecentPosts(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(post)));
        when(likeRepository.countByPostIdIn(any())).thenReturn(Collections.singletonList(new Object[]{1L, 2L}));
        when(commentRepository.countByPostIdIn(any())).thenReturn(Collections.singletonList(new Object[]{1L, 3L}));

        var feed = feedService.getHomeFeed(0, 20, "posts", "date").getContent();

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).getCommentCount()).isEqualTo(3);
        assertThat(feed.get(0).getLikeCount()).isEqualTo(2);
        assertThat(feed.get(0).getIsLikedByCurrentUser()).isFalse();
    }

    @Test
    void feedStatsStillCountsPublishedUpcomingEvents() {
        when(postRepository.count()).thenReturn(5L);
        when(eventRepository.countByStatusAndDateAfter(eq(EventStatus.PUBLISHED), any(LocalDateTime.class))).thenReturn(0L);
        when(eventRepository.countByPaidTrue()).thenReturn(1L);

        FeedService.FeedStats stats = feedService.getFeedStats();

        assertThat(stats.getTotalPosts()).isEqualTo(5L);
        assertThat(stats.getTotalEvents()).isZero();
        assertThat(stats.getPaidEvents()).isEqualTo(1L);
    }
}
