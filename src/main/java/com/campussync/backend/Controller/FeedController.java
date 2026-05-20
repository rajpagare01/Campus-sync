package com.campussync.backend.Controller;

import com.campussync.backend.Dto.FeedItem;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/feed", "/api/v1/feed"})
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * Get personalized home feed
     * GET /feed?page=0&size=20&filter=all&sort=date
     *
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20, max: 50)
     * @param filter Filter type: "all", "posts", "events" (default: "all")
     * @param sort Sort type: "date", "engagement" (default: "date")
     * @return List of FeedItem objects
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<FeedItem>> getHomeFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "date") String sort) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50; // Max page size

        // Validate filter
        if (!List.of("all", "posts", "events").contains(filter)) {
            filter = "all";
        }

        // Validate sort
        if (!List.of("date", "engagement").contains(sort)) {
            sort = "date";
        }

        PaginatedResponse<FeedItem> feed = feedService.getHomeFeed(page, size, filter, sort);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get feed statistics
     * GET /feed/stats
     *
     * @return Feed statistics including counts
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFeedStats() {
        FeedService.FeedStats stats = feedService.getFeedStats();

        Map<String, Object> response = new HashMap<>();
        response.put("totalPosts", stats.getTotalPosts());
        response.put("totalEvents", stats.getTotalEvents());
        response.put("paidEvents", stats.getPaidEvents());
        response.put("timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Get posts-only feed (alternative endpoint)
     * GET /feed/posts?page=0&size=20&sort=date
     */
    @GetMapping("/posts")
    public ResponseEntity<PaginatedResponse<FeedItem>> getPostsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sort) {

        PaginatedResponse<FeedItem> feed = feedService.getHomeFeed(page, size, "posts", sort);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get events-only feed (alternative endpoint)
     * GET /feed/events?page=0&size=20&sort=date
     */
    @GetMapping("/events")
    public ResponseEntity<PaginatedResponse<FeedItem>> getEventsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sort) {

        PaginatedResponse<FeedItem> feed = feedService.getHomeFeed(page, size, "events", sort);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get trending feed (engagement-based wrapper)
     * GET /feed/trending?page=0&size=20&filter=all
     */
    @GetMapping("/trending")
    public ResponseEntity<PaginatedResponse<FeedItem>> getTrendingFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String filter) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50;

        // Trending feed inherently sorts by engagement
        PaginatedResponse<FeedItem> feed = feedService.getHomeFeed(page, size, filter, "engagement");
        return ResponseEntity.ok(feed);
    }
}
