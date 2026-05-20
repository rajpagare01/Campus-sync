package com.campussync.backend.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;

import com.campussync.backend.Dto.PlatformStats;
import com.campussync.backend.Dto.UserRetentionMetrics;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getPlatformStatsSeparatesTotalLikesFromLikesToday() {
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.countByUpdatedAtAfter(any(LocalDateTime.class))).thenReturn(2L);
        when(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(3L);
        when(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);
        when(postRepository.count()).thenReturn(10L);
        when(postRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(4L);
        when(commentRepository.count()).thenReturn(8L);
        when(commentRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(2L);
        when(likeRepository.count()).thenReturn(12L);
        when(likeRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(4L);
        when(likeRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);
        when(eventRepository.count()).thenReturn(3L);
        when(eventRepository.countByDateAfter(any(LocalDateTime.class))).thenReturn(1L);

        PlatformStats stats = analyticsService.getPlatformStats();

        assertThat(stats.getTotalLikes()).isEqualTo(12L);
        assertThat(stats.getLikesToday()).isEqualTo(4L);
    }

    @Test
    void getTrendingEventsUsesEventDateSortProperty() {
        when(eventRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        analyticsService.getTrendingEvents(5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("date")).isNotNull();
    }

    @Test
    void getUserRetentionCountsOnlyUsersFromTheRequestedCohort() {
        when(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(4L);
        when(userRepository.countByCreatedAtBetweenAndUpdatedAtAfter(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(2L);

        UserRetentionMetrics metrics = analyticsService.getUserRetention(LocalDateTime.now().minusDays(30));

        assertThat(metrics.getCohortSize()).isEqualTo(4L);
        assertThat(metrics.getRetainedDay7()).isEqualTo(2L);
        assertThat(metrics.getRetentionRateDay7()).isEqualTo(0.5);
        verify(userRepository, never()).countByCreatedAtBeforeAndUpdatedAtAfter(any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
