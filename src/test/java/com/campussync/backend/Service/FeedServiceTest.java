package com.campussync.backend.Service;

import com.campussync.backend.Dto.FeedItem;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private LikeService likeService;

    @Mock
    private CommentService commentService;

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

        when(postRepository.findRecentPosts()).thenReturn(List.of(post));
        when(commentService.getCommentCount(1L)).thenReturn(3);
        when(likeService.getLikeCount(1L)).thenReturn(2);
        when(likeService.hasUserLikedPost(1L)).thenReturn(true);

        List<FeedItem> feed = feedService.getHomeFeed(0, 20, "posts", "date");

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).getCommentCount()).isEqualTo(3);
        assertThat(feed.get(0).getLikeCount()).isEqualTo(2);
        assertThat(feed.get(0).getIsLikedByCurrentUser()).isTrue();
    }

    @Test
    void feedStatsStillCountsPublishedUpcomingEvents() {
        when(postRepository.count()).thenReturn(5L);
        when(eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of());
        when(eventRepository.countByPaidTrue()).thenReturn(1L);

        FeedService.FeedStats stats = feedService.getFeedStats();

        assertThat(stats.getTotalPosts()).isEqualTo(5L);
        assertThat(stats.getTotalEvents()).isZero();
        assertThat(stats.getPaidEvents()).isEqualTo(1L);
    }
}
