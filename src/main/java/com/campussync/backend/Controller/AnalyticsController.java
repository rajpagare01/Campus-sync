package com.campussync.backend.Controller;

import com.campussync.backend.Dto.*;
import com.campussync.backend.Service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/analytics", "/api/v1/analytics"})
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    // ==================== User Engagement Endpoints ====================

    @GetMapping("/users/engagement")
    public ResponseEntity<UserEngagementMetrics> getUserEngagement(
            @RequestParam(defaultValue = "30") int days) {
        
        if (days <= 0 || days > 365) {
            return ResponseEntity.badRequest().build();
        }
        
        UserEngagementMetrics metrics = analyticsService.getUserEngagementMetrics(days);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/users/daily-stats")
    public ResponseEntity<List<DailyUserStats>> getDailyStats(
            @RequestParam(defaultValue = "30") int days) {
        
        if (days <= 0 || days > 365) {
            return ResponseEntity.badRequest().build();
        }
        
        List<DailyUserStats> stats = analyticsService.getDailyStats(days);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users/retention")
    public ResponseEntity<UserRetentionMetrics> getUserRetention(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cohortDate) {
        
        UserRetentionMetrics metrics = analyticsService.getUserRetention(cohortDate);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/users/growth")
    public ResponseEntity<Map<String, Object>> getUserGrowth(
            @RequestParam(defaultValue = "30") int days) {
        
        if (days <= 0 || days > 365) {
            return ResponseEntity.badRequest().build();
        }
        
        UserEngagementMetrics currentMetrics = analyticsService.getUserEngagementMetrics(days);
        UserEngagementMetrics previousMetrics = analyticsService.getUserEngagementMetrics(days * 2);

        Map<String, Object> growth = new HashMap<>();
        growth.put("current_period", currentMetrics);
        growth.put("previous_period", previousMetrics);
        growth.put("user_growth_rate", 
            (currentMetrics.getTotalUsers() - previousMetrics.getTotalUsers()) / 
            (double) Math.max(previousMetrics.getTotalUsers(), 1));
        growth.put("engagement_change", 
            currentMetrics.getEngagementRate() - previousMetrics.getEngagementRate());

        return ResponseEntity.ok(growth);
    }

    // ==================== Event Analytics Endpoints ====================

    @GetMapping("/events/trending")
    public ResponseEntity<List<EventAnalytics>> getTrendingEvents(
            @RequestParam(defaultValue = "10") int limit) {
        
        if (limit <= 0 || limit > MAX_PAGE_SIZE) {
            limit = DEFAULT_PAGE_SIZE;
        }
        
        List<EventAnalytics> events = analyticsService.getTrendingEvents(limit);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{id}/stats")
    public ResponseEntity<EventAnalytics> getEventStats(@PathVariable Long id) {
        try {
            EventAnalytics analytics = analyticsService.getEventAnalytics(id);
            return ResponseEntity.ok(analytics);
        } catch (RuntimeException e) {
            log.warn("Event not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/events/category-stats")
    public ResponseEntity<Map<String, Long>> getEventCategoryStats() {
        Map<String, Long> stats = analyticsService.getEventsCategoryStats();
        return ResponseEntity.ok(stats);
    }

    // ==================== Content Analytics Endpoints ====================

    @GetMapping("/posts/{id}/performance")
    public ResponseEntity<PostPerformanceMetrics> getPostPerformance(@PathVariable Long id) {
        try {
            PostPerformanceMetrics metrics = analyticsService.getPostPerformance(id);
            return ResponseEntity.ok(metrics);
        } catch (RuntimeException e) {
            log.warn("Post not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/content/trends")
    public ResponseEntity<List<ContentTrend>> getContentTrends(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        
        if (days <= 0 || days > 365) {
            return ResponseEntity.badRequest().build();
        }
        
        if (limit <= 0 || limit > MAX_PAGE_SIZE) {
            limit = DEFAULT_PAGE_SIZE;
        }
        
        List<ContentTrend> trends = analyticsService.getTrendingHashtags(days, limit);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/content/trending-posts")
    public ResponseEntity<List<PostPerformanceMetrics>> getTrendingPosts(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        
        if (days <= 0 || days > 365) {
            return ResponseEntity.badRequest().build();
        }
        
        if (limit <= 0 || limit > MAX_PAGE_SIZE) {
            limit = DEFAULT_PAGE_SIZE;
        }
        
        List<PostPerformanceMetrics> posts = analyticsService.getTrendingPosts(days, limit);
        return ResponseEntity.ok(posts);
    }

    // ==================== Platform Statistics Endpoints ====================

    @GetMapping("/platform/stats")
    public ResponseEntity<PlatformStats> getPlatformStats() {
        PlatformStats stats = analyticsService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/platform/daily")
    public ResponseEntity<List<DailyUserStats>> getPlatformDailyStats(
            @RequestParam(defaultValue = "30") int days) {
        
        if (days <= 0 || days > 365) {
            return ResponseEntity.badRequest().build();
        }
        
        List<DailyUserStats> stats = analyticsService.getDailyStats(days);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/platform/growth")
    public ResponseEntity<Map<String, Object>> getPlatformGrowth() {
        PlatformStats currentStats = analyticsService.getPlatformStats();
        UserEngagementMetrics engagementMetrics = analyticsService.getUserEngagementMetrics(30);

        Map<String, Object> growth = new HashMap<>();
        growth.put("platform_stats", currentStats);
        growth.put("engagement_metrics", engagementMetrics);
        growth.put("growth_rate", currentStats.getUserGrowthRate());
        growth.put("health_score", currentStats.getPlatformHealthScore());

        return ResponseEntity.ok(growth);
    }

    @GetMapping("/platform/summary")
    public ResponseEntity<Map<String, Object>> getPlatformSummary() {
        PlatformStats stats = analyticsService.getPlatformStats();
        UserEngagementMetrics engagement = analyticsService.getUserEngagementMetrics(30);
        List<EventAnalytics> trendingEvents = analyticsService.getTrendingEvents(5);
        List<PostPerformanceMetrics> trendingPosts = analyticsService.getTrendingPosts(7, 5);

        Map<String, Object> summary = new HashMap<>();
        summary.put("platform_stats", stats);
        summary.put("engagement_metrics", engagement);
        summary.put("trending_events", trendingEvents);
        summary.put("trending_posts", trendingPosts);
        summary.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(summary);
    }

    // ==================== Error Handling ====================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        log.error("Error in analytics endpoint", e);
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
