package com.campussync.backend.Repository;

import com.campussync.backend.Dto.*;
import com.campussync.backend.Model.*;

import com.campussync.backend.Service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsServiceTest {



    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private EventRepository eventRepository;

    private User testUser;
    private Post testPost;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setBio("Test bio");
        testUser = userRepository.save(testUser);

        // Create test post
        testPost = new Post();
        testPost.setAuthor(testUser);
        testPost.setContent("Test post content");
        testPost = postRepository.save(testPost);

        // Create test event
        testEvent = new Event();
        testEvent.setTitle("Test Event");
        testEvent.setDescription("Test event description");
        testEvent.setVenue("Test Venue");
        testEvent.setDate(LocalDateTime.now().plusDays(7));
        testEvent.setType(EventType.SOCIETY);
        testEvent.setCreatedBy(testUser);
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);
    }

    // ==================== User Engagement Metrics Tests ====================

    @Test
    void testGetUserEngagementMetrics() {
        UserEngagementMetrics metrics = analyticsService.getUserEngagementMetrics(30);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalUsers()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.getEngagementRate()).isBetween(0.0, 100.0);
        assertThat(metrics.getPostsPerUser()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getLastUpdated()).isNotNull();
    }

    @Test
    void testGetEventAnalytics() {
        EventAnalytics analytics = analyticsService.getEventAnalytics(testEvent.getId());

        assertThat(analytics).isNotNull();
        assertThat(analytics.getEventId()).isEqualTo(testEvent.getId());
        assertThat(analytics.getEventTitle()).isEqualTo("Test Event");
        assertThat(analytics.getAttendanceRate()).isBetween(0.0, 1.0);
        assertThat(analytics.getTotalPosts()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testGetPostPerformance() {
        PostPerformanceMetrics metrics = analyticsService.getPostPerformance(testPost.getId());

        assertThat(metrics).isNotNull();
        assertThat(metrics.getPostId()).isEqualTo(testPost.getId());
        assertThat(metrics.getUserId()).isEqualTo(testUser.getId());
        assertThat(metrics.getTotalLikes()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getTotalComments()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testGetPlatformStats() {
        PlatformStats stats = analyticsService.getPlatformStats();

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalUsers()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getTotalPosts()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getAveragePostsPerUser()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getLastUpdated()).isNotNull();
    }

    @Test
    void testGetDailyStats() {
        List<DailyUserStats> stats = analyticsService.getDailyStats(7);

        assertThat(stats).isNotNull();
        assertThat(stats.size()).isEqualTo(7);
        for (DailyUserStats stat : stats) {
            assertThat(stat.getDate()).isNotNull();
            assertThat(stat.getNewUsers()).isGreaterThanOrEqualTo(0);
            assertThat(stat.getDailyActiveUsers()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void testGetTrendingEvents() {
        List<EventAnalytics> events = analyticsService.getTrendingEvents(10);

        assertThat(events).isNotNull();
        assertThat(events.size()).isLessThanOrEqualTo(10);
    }

    @Test
    void testGetEventsCategoryStats() {
        Map<String, Long> stats = analyticsService.getEventsCategoryStats();

        assertThat(stats).isNotNull();
        assertThat(stats).containsKey("SOCIETY");
    }

    @Test
    void testGetTrendingPosts() {
        List<PostPerformanceMetrics> posts = analyticsService.getTrendingPosts(30, 10);

        assertThat(posts).isNotNull();
        assertThat(posts.size()).isLessThanOrEqualTo(10);
    }

    @Test
    void testGetTrendingHashtags() {
        // Create post with hashtags
        Post hashtagPost = new Post();
        hashtagPost.setAuthor(testUser);
        hashtagPost.setContent("Great post #java #spring #backend");
        postRepository.save(hashtagPost);

        List<ContentTrend> trends = analyticsService.getTrendingHashtags(30, 10);

        assertThat(trends).isNotNull();
    }

    @Test
    void testGetUserRetention() {
        LocalDateTime cohortDate = LocalDateTime.now().minusDays(30);
        UserRetentionMetrics retention = analyticsService.getUserRetention(cohortDate);

        assertThat(retention).isNotNull();
        assertThat(retention.getCohortDate()).isNotNull();
        assertThat(retention.getRetentionRateDay7()).isBetween(0.0, 1.0);
        assertThat(retention.getRetentionRateDay30()).isBetween(0.0, 1.0);
    }

    @Test
    void testFullAnalyticsWorkflow() {
        // Get all metrics
        UserEngagementMetrics engagement = analyticsService.getUserEngagementMetrics(30);
        PlatformStats platform = analyticsService.getPlatformStats();
        List<DailyUserStats> daily = analyticsService.getDailyStats(7);

        assertThat(engagement).isNotNull();
        assertThat(platform).isNotNull();
        assertThat(daily).isNotNull();

        // Verify consistency
        assertThat(engagement.getTotalUsers()).isEqualTo(platform.getTotalUsers());
    }
}
