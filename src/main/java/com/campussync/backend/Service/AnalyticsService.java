package com.campussync.backend.Service;

import com.campussync.backend.Dto.*;
import com.campussync.backend.Model.Comment;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Like;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final EventRepository eventRepository;

    // ==================== User Engagement Metrics ====================

    public UserEngagementMetrics getUserEngagementMetrics(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime now = LocalDateTime.now();

        long totalUsers = userRepository.count();
        long activeUsers7d = userRepository.countByUpdatedAtAfter(
            LocalDateTime.now().minusDays(7)
        );
        long activeUsers30d = userRepository.countByUpdatedAtAfter(
            LocalDateTime.now().minusDays(30)
        );
        long newUsersToday = userRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(1)
        );
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(30)
        );

        long totalPosts = postRepository.count();
        long totalComments = commentRepository.count();
        long totalLikes = likeRepository.count();

        double engagementRate = totalUsers > 0 ? 
            (double) (totalPosts + totalComments) / totalUsers : 0.0;
        double postsPerUser = totalUsers > 0 ? (double) totalPosts / totalUsers : 0.0;
        double commentsPerUser = totalUsers > 0 ? (double) totalComments / totalUsers : 0.0;
        double likesPerUser = totalUsers > 0 ? (double) totalLikes / totalUsers : 0.0;

        long dailyActiveUsers = userRepository.countByUpdatedAtAfter(
            LocalDateTime.now().minusDays(1)
        );
        long monthlyActiveUsers = activeUsers30d;

        return UserEngagementMetrics.builder()
            .totalUsers(totalUsers)
            .activeUsers7d(activeUsers7d)
            .activeUsers30d(activeUsers30d)
            .newUsersToday(newUsersToday)
            .newUsersThisMonth(newUsersThisMonth)
            .engagementRate(Math.round(engagementRate * 100.0) / 100.0)
            .postsPerUser(Math.round(postsPerUser * 100.0) / 100.0)
            .commentsPerUser(Math.round(commentsPerUser * 100.0) / 100.0)
            .likesPerUser(Math.round(likesPerUser * 100.0) / 100.0)
            .dailyActiveUsers(dailyActiveUsers)
            .monthlyActiveUsers(monthlyActiveUsers)
            .retentionRateDay7(calculateRetentionRate(7))
            .retentionRateDay30(calculateRetentionRate(30))
            .periodStart(startDate)
            .periodEnd(now)
            .lastUpdated(now)
            .build();
    }

    // ==================== Event Analytics ====================

    public EventAnalytics getEventAnalytics(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        long registrations = eventRepository.countEventAttendees(eventId);
        long attendees = eventRepository.countEventActualAttendees(eventId, RegistrationStatus.CANCELLED);
        double attendanceRate = registrations > 0 ? (double) attendees / registrations : 0.0;

        long postsCount = postRepository.countByLinkedEvent_Id(eventId);
        long commentsCount = commentRepository.countByPostEventId(eventId);
        long likesCount = likeRepository.countByPostEventId(eventId);

        double totalEngagement = postsCount + commentsCount + likesCount;
        double engagementRate = registrations > 0 ? totalEngagement / registrations : 0.0;

        double avgLikesPerPost = postsCount > 0 ? (double) likesCount / postsCount : 0.0;
        double avgCommentsPerPost = postsCount > 0 ? (double) commentsCount / postsCount : 0.0;

        return EventAnalytics.builder()
            .eventId(eventId)
            .eventTitle(event.getTitle())
            .eventCategory("General")
            .totalRegistrations(registrations)
            .actualAttendees(attendees)
            .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
            .totalPosts(postsCount)
            .totalComments(commentsCount)
            .totalLikes(likesCount)
            .engagementRate(Math.round(engagementRate * 100.0) / 100.0)
            .averageLikesPerPost(Math.round(avgLikesPerPost * 100.0) / 100.0)
            .averageCommentsPerPost(Math.round(avgCommentsPerPost * 100.0) / 100.0)
            .eventDate(event.getDate())
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    public List<EventAnalytics> getTrendingEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("date").descending());
        List<Event> events = eventRepository.findAll(pageable).getContent();

        return events.stream()
            .map(event -> getEventAnalytics(event.getId()))
            .sorted((a, b) -> Double.compare(b.getEngagementRate(), a.getEngagementRate()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public Map<String, Long> getEventsCategoryStats() {
        List<Event> events = eventRepository.findAll();
        // Bug #12 Fix: use actual event type instead of hardcoded "General"
        return events.stream()
            .filter(event -> event.getType() != null)
            .collect(Collectors.groupingBy(
                event -> event.getType().name(),
                Collectors.counting()
            ));
    }

    // ==================== Post Performance Metrics ====================

    public PostPerformanceMetrics getPostPerformance(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));

        long likesCount = likeRepository.countByPostId(postId);
        long commentsCount = commentRepository.countByPostId(postId);
        long sharesCount = 0;

        double engagementRate = (likesCount + commentsCount) > 0 ? 1.0 : 0.0;
        double engagementScore = (likesCount * 1.0) + (commentsCount * 2.0);

        return PostPerformanceMetrics.builder()
            .postId(postId)
            .userId(post.getAuthor().getId())
            .userName(post.getAuthor().getName())
            .contentPreview(truncateContent(post.getContent(), 100))
            .totalLikes(likesCount)
            .totalComments(commentsCount)
            .totalShares(sharesCount)
            .totalViews(0L)
            .engagementRate(Math.round(engagementRate * 100.0) / 100.0)
            .engagementScore(Math.round(engagementScore * 100.0) / 100.0)
            .postDate(post.getCreatedAt())
            .trending(engagementScore > 10.0)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    public List<PostPerformanceMetrics> getTrendingPosts(int days, int limit) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Post> posts = postRepository.findByCreatedAtAfter(startDate);

        return posts.stream()
            .map(post -> getPostPerformance(post.getId()))
            .sorted((a, b) -> Double.compare(b.getEngagementScore(), a.getEngagementScore()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ContentTrend> getTrendingHashtags(int days, int limit) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Post> posts = postRepository.findByCreatedAtAfter(startDate);

        Map<String, Long> hashtagCounts = new HashMap<>();
        for (Post post : posts) {
            String content = post.getContent();
            if (content != null) {
                String[] words = content.split("\\s+");
                for (String word : words) {
                    if (word.startsWith("#")) {
                        hashtagCounts.put(word, hashtagCounts.getOrDefault(word, 0L) + 1);
                    }
                }
            }
        }

        return hashtagCounts.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(entry -> ContentTrend.builder()
                .trendId(entry.getKey())
                .contentType("hashtag")
                .trendName(entry.getKey())
                .occurrenceCount(entry.getValue())
                .trendScore((double) entry.getValue())
                .isTrending(entry.getValue() > 5)
                .firstSeen(LocalDateTime.now().minusDays(days))
                .lastSeen(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build())
            .collect(Collectors.toList());
    }

    // ==================== Platform Statistics ====================

    public PlatformStats getPlatformStats() {
        long totalUsers = userRepository.count();
        long activeUsers7d = userRepository.countByUpdatedAtAfter(
            LocalDateTime.now().minusDays(7)
        );
        long activeUsers30d = userRepository.countByUpdatedAtAfter(
            LocalDateTime.now().minusDays(30)
        );
        long dailyActiveUsers = userRepository.countByUpdatedAtAfter(
            LocalDateTime.now().minusDays(1)
        );

        long totalPosts = postRepository.count();
        long postsToday = postRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(1)
        );
        long postsThisMonth = postRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(30)
        );

        long totalEvents = eventRepository.count();
        long activeEvents = eventRepository.countByDateAfter(LocalDateTime.now());

        long totalComments = commentRepository.count();
        long commentsToday = commentRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(1)
        );

        long totalLikes = likeRepository.count();
        long likesToday = likeRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(1)
        );

        double avgPostsPerUser = totalUsers > 0 ? (double) totalPosts / totalUsers : 0.0;
        double avgEngagementRate = totalPosts > 0 ? 
            (double) (totalComments + totalLikes) / totalPosts : 0.0;

        double userGrowthRate = calculateUserGrowthRate();
        String engagementTrend = determineEngagementTrend();
        double platformHealthScore = calculatePlatformHealthScore(
            totalUsers, dailyActiveUsers, totalPosts, avgEngagementRate
        );

        return PlatformStats.builder()
            .totalUsers(totalUsers)
            .activeUsers7d(activeUsers7d)
            .activeUsers30d(activeUsers30d)
            .dailyActiveUsers(dailyActiveUsers)
            .totalPosts(totalPosts)
            .postsToday(postsToday)
            .postsThisMonth(postsThisMonth)
            .totalEvents(totalEvents)
            .activeEvents(activeEvents)
            .totalComments(totalComments)
            .commentsToday(commentsToday)
            .totalLikes(totalLikes)
            .likesToday(likesToday)
            .averagePostsPerUser(Math.round(avgPostsPerUser * 100.0) / 100.0)
            .averageEngagementRate(Math.round(avgEngagementRate * 100.0) / 100.0)
            .userGrowthRate(Math.round(userGrowthRate * 100.0) / 100.0)
            .engagementTrend(engagementTrend)
            .platformHealthScore(Math.round(platformHealthScore * 100.0) / 100.0)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    public List<DailyUserStats> getDailyStats(int days) {
        List<DailyUserStats> stats = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long newUsers = userRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long dau = userRepository.countByUpdatedAtBetween(dayStart, dayEnd);
            long postsCreated = postRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long commentsCreated = commentRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long likesCreated = likeRepository.countByCreatedAtBetween(dayStart, dayEnd);

            long totalEngagement = postsCreated + commentsCreated + likesCreated;
            double engagementRate = dau > 0 ? (double) totalEngagement / dau : 0.0;

            long newEvents = eventRepository.countByCreatedAtBetween(dayStart, dayEnd);

            stats.add(DailyUserStats.builder()
                .date(date)
                .newUsers(newUsers)
                .dailyActiveUsers(dau)
                .totalPosts(postsCreated)
                .totalComments(commentsCreated)
                .totalLikes(likesCreated)
                .totalEngagement(totalEngagement)
                .engagementRate(Math.round(engagementRate * 100.0) / 100.0)
                .postsCreated(postsCreated)
                .commentsCreated(commentsCreated)
                .likesCreated(likesCreated)
                .newEvents(newEvents)
                .lastUpdated(LocalDateTime.now())
                .build());
        }

        return stats;
    }

    // ==================== User Retention ====================

    public UserRetentionMetrics getUserRetention(LocalDateTime cohortDate) {
        LocalDateTime dayAfter = cohortDate.plusDays(1);
        LocalDateTime day7After = cohortDate.plusDays(7);
        LocalDateTime day30After = cohortDate.plusDays(30);
        LocalDateTime day90After = cohortDate.plusDays(90);

        long cohortSize = userRepository.countByCreatedAtBetween(
            cohortDate.toLocalDate().atStartOfDay(),
            cohortDate.toLocalDate().atTime(LocalTime.MAX)
        );

        long retainedDay1 = calculateRetainedUsersForCohort(cohortDate, dayAfter);
        long retainedDay7 = calculateRetainedUsersForCohort(cohortDate, day7After);
        long retainedDay30 = calculateRetainedUsersForCohort(cohortDate, day30After);
        long retainedDay90 = calculateRetainedUsersForCohort(cohortDate, day90After);

        double retRate1 = cohortSize > 0 ? (double) retainedDay1 / cohortSize : 0.0;
        double retRate7 = cohortSize > 0 ? (double) retainedDay7 / cohortSize : 0.0;
        double retRate30 = cohortSize > 0 ? (double) retainedDay30 / cohortSize : 0.0;
        double retRate90 = cohortSize > 0 ? (double) retainedDay90 / cohortSize : 0.0;

        return UserRetentionMetrics.builder()
            .cohortDate(cohortDate)
            .cohortSize(cohortSize)
            .retainedDay1(retainedDay1)
            .retainedDay7(retainedDay7)
            .retainedDay30(retainedDay30)
            .retainedDay90(retainedDay90)
            .retentionRateDay1(Math.round(retRate1 * 100.0) / 100.0)
            .retentionRateDay7(Math.round(retRate7 * 100.0) / 100.0)
            .retentionRateDay30(Math.round(retRate30 * 100.0) / 100.0)
            .retentionRateDay90(Math.round(retRate90 * 100.0) / 100.0)
            .churnRate(Math.round((1.0 - retRate30) * 100.0) / 100.0)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    // ==================== Helper Methods ====================

    private double calculateRetentionRate(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        long activeUsers = userRepository.countByUpdatedAtAfter(startDate);
        long totalUsers = userRepository.count();
        return totalUsers > 0 ? (double) activeUsers / totalUsers : 0.0;
    }

    private long calculateRetainedUsersForCohort(LocalDateTime cohortStart, LocalDateTime checkDate) {
        LocalDateTime cohortDayStart = cohortStart.toLocalDate().atStartOfDay();
        LocalDateTime cohortDayEnd = cohortStart.toLocalDate().atTime(LocalTime.MAX);

        return userRepository.countByCreatedAtBetweenAndUpdatedAtAfter(
            cohortDayStart,
            cohortDayEnd,
            checkDate
        );
    }

    private double calculateUserGrowthRate() {
        long thisMonth = userRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(30)
        );
        long lastMonth = userRepository.countByCreatedAtBetween(
            LocalDateTime.now().minusDays(60),
            LocalDateTime.now().minusDays(30)
        );

        return lastMonth > 0 ? ((double) thisMonth - lastMonth) / lastMonth : 0.0;
    }

    private String determineEngagementTrend() {
        long today = likeRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(1)
        );
        long yesterday = likeRepository.countByCreatedAtBetween(
            LocalDateTime.now().minusDays(2),
            LocalDateTime.now().minusDays(1)
        );

        if (today > yesterday) return "INCREASING";
        if (today < yesterday) return "DECREASING";
        return "STABLE";
    }

    private double calculatePlatformHealthScore(
        long totalUsers, long dailyActiveUsers, long totalPosts, double engagementRate) {
        
        double userScore = Math.min((double) dailyActiveUsers / Math.max(totalUsers, 1) * 100, 40);
        double activityScore = Math.min((double) totalPosts / Math.max(totalUsers, 1) * 10, 40);
        double engagementScore = Math.min(engagementRate * 20, 20);

        return userScore + activityScore + engagementScore;
    }

    private String truncateContent(String content, int length) {
        if (content == null) return "";
        return content.length() > length ? 
            content.substring(0, length) + "..." : content;
    }
}
