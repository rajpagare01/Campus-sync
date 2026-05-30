package com.campussync.backend.Service;

import com.campussync.backend.Dto.FeedItem;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final EventRepository eventRepository;
    private final LikeService likeService;
    private final CommentService commentService;
    private final FollowService followService;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    /**
     * Get personalized home feed combining posts and events
     * @param page Page number (0-based)
     * @param size Page size
     * @param filter Filter type: "all", "posts", "events", "recent"
     * @param sort Sort type: "date", "engagement"
     * @return List of FeedItem objects
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<FeedItem> getHomeFeed(int page, int size, String filter, String sort) {
        Pageable pageable = PageRequest.of(page, size);

        List<FeedItem> feedItems = new ArrayList<>();

        // Fetch followed IDs for personalization
        List<Long> followedIds = new ArrayList<>();
        try {
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                followedIds = followService.getFollowingIds(currentUser.getId());
            }
        } catch (Exception e) {
            // Ignore if not authenticated
        }

        // Fetch posts based on filter
        if ("all".equals(filter) || "posts".equals(filter)) {
            List<Post> posts = getPostsForFeed(pageable);
            feedItems.addAll(convertPostsToFeedItems(posts, followedIds));
        }

        // Fetch events based on filter
        if ("all".equals(filter) || "events".equals(filter)) {
            List<Event> events = getEventsForFeed(pageable);
            feedItems.addAll(convertEventsToFeedItems(events, followedIds));
        }

        // Apply sorting
        if ("engagement".equals(sort)) {
            feedItems.sort(Comparator.comparing(FeedItem::getEngagementScore).reversed());
        } else {
            // Default: sort by creation date (newest first)
            feedItems.sort(Comparator.comparing(FeedItem::getCreatedAt).reversed());
        }

        // Handle pagination for the merged results
        // Since we already paged individual sources, the merged list contains up to 2*size items.
        // We trim it to size.
        List<FeedItem> pagedItems;
        if (feedItems.size() > size) {
            pagedItems = feedItems.subList(0, size);
        } else {
            pagedItems = feedItems;
        }

        // Calculate total elements for the selected filter
        long totalElements;
        if ("posts".equals(filter)) {
            totalElements = postRepository.count();
        } else if ("events".equals(filter)) {
            totalElements = eventRepository.countByStatus(EventStatus.PUBLISHED); // Simplified
        } else {
            totalElements = postRepository.count() + eventRepository.countByStatus(EventStatus.PUBLISHED);
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean isLast = pagedItems.size() < size || (page + 1) >= totalPages;

        return new PaginatedResponse<>(
                pagedItems,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                isLast,
                pagedItems.isEmpty()
        );
    }

    private List<Post> getPostsForFeed(Pageable pageable) {
        return postRepository.findRecentPosts(pageable).getContent();
    }

    /**
     * Get upcoming events for feed
     */
    private List<Event> getEventsForFeed(Pageable pageable) {
        return eventRepository.findUpcomingEvents(EventStatus.PUBLISHED, LocalDateTime.now(), pageable).getContent();
    }

    /**
     * Convert posts to feed items
     */
    private List<FeedItem> convertPostsToFeedItems(List<Post> posts, List<Long> followedIds) {
        if (posts.isEmpty()) return new ArrayList<>();
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        
        java.util.Map<Long, Integer> likeMap = new java.util.HashMap<>();
        for (Object[] row : likeRepository.countByPostIdIn(postIds)) {
            likeMap.put((Long) row[0], ((Number) row[1]).intValue());
        }
        
        java.util.Map<Long, Integer> commentMap = new java.util.HashMap<>();
        for (Object[] row : commentRepository.countByPostIdIn(postIds)) {
            commentMap.put((Long) row[0], ((Number) row[1]).intValue());
        }
        
        java.util.Set<Long> userLikedPostIds = new java.util.HashSet<>();
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            for (com.campussync.backend.Model.Like like : likeRepository.findByUserIdAndPostIdIn(currentUser.getId(), postIds)) {
                userLikedPostIds.add(like.getPost().getId());
            }
        }

        return posts.stream().map(post -> {
            FeedItem item = new FeedItem();
            item.setType("POST");
            item.setId(post.getId());
            item.setCreatedAt(post.getCreatedAt());
            item.setTitle("Post by " + post.getAuthor().getName());
            item.setContent(post.getContent());
            item.setAuthorName(post.getAuthor().getName());
            item.setAuthorId(post.getAuthor().getId());
            item.setMediaUrl(post.getMediaUrl());
            
            int likes = likeMap.getOrDefault(post.getId(), 0);
            int comments = commentMap.getOrDefault(post.getId(), 0);
            boolean liked = userLikedPostIds.contains(post.getId());
            
            item.setLikeCount(likes);
            item.setLikesCount(likes);
            item.setCommentCount(comments);
            item.setCommentsCount(comments);
            item.setIsLikedByCurrentUser(liked);
            item.setLiked(liked);

            // Populate author details
            User author = post.getAuthor();
            if (author != null) {
                String fullName = author.getName() != null ? author.getName().trim() : "";
                String[] parts = fullName.split("\\s+", 2);
                item.setAuthorFirstName(parts.length > 0 ? parts[0] : "");
                item.setAuthorLastName(parts.length > 1 ? parts[1] : "");
                item.setAuthorProfilePicture(author.getProfilePictureUrl());
                item.setProfilePictureUrl(author.getProfilePictureUrl());
            }

            double recencyScore = calculateRecencyScore(post.getCreatedAt());
            double baseScore = (item.getLikeCount() * 1.5) + (item.getCommentCount() * 2.0) + recencyScore;
            double followMultiplier = followedIds.contains(post.getAuthor().getId()) ? 1.5 : 1.0;

            item.setEngagementScore(baseScore * followMultiplier);

            return item;
        }).collect(Collectors.toList());
    }



    private List<FeedItem> convertEventsToFeedItems(List<Event> events, List<Long> followedIds) {
        if (events.isEmpty()) return new ArrayList<>();
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        java.util.Map<Long, Long> regMap = new java.util.HashMap<>();
        for (Object[] row : registrationRepository.countByEventIdInAndStatus(eventIds, com.campussync.backend.Model.RegistrationStatus.REGISTERED)) {
            regMap.put((Long) row[0], ((Number) row[1]).longValue());
        }

        return events.stream().map(event -> {
            FeedItem item = new FeedItem();
            item.setType("EVENT");
            item.setId(event.getId());

            item.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : event.getDate());
            item.setTitle(event.getTitle());
            item.setContent(event.getDescription());
            item.setAuthorName(event.getCreatedBy() != null ? event.getCreatedBy().getName() : "Unknown");
            item.setAuthorId(event.getCreatedBy() != null ? event.getCreatedBy().getId() : null);
            item.setVenue(event.getVenue());
            item.setEventDate(event.getDate());
            item.setEventType(event.getType() != null ? event.getType().name() : null);
            item.setPaid(event.getPaid());
            item.setPrice(event.getPrice());
            item.setImageUrl(event.getImageUrl());
            item.setRegistrationCount(regMap.getOrDefault(event.getId(), 0L));

            // Populate author details
            User creator = event.getCreatedBy();
            if (creator != null) {
                String fullName = creator.getName() != null ? creator.getName().trim() : "";
                String[] parts = fullName.split("\\s+", 2);
                item.setAuthorFirstName(parts.length > 0 ? parts[0] : "");
                item.setAuthorLastName(parts.length > 1 ? parts[1] : "");
                item.setAuthorProfilePicture(creator.getProfilePictureUrl());
                item.setProfilePictureUrl(creator.getProfilePictureUrl());
            }

            // Bug #1 Fix: Use createdAt (when published) for recency scoring,
            // NOT event.getDate() (future occurrence date) which inflated scores.
            LocalDateTime recencyBase = event.getCreatedAt() != null ? event.getCreatedAt() : event.getDate();
            double recencyScore = calculateRecencyScore(recencyBase);
            double typeBonus = event.getType() == EventType.PLACEMENT ? 5.0 : 2.0;
            double baseScore = recencyScore + typeBonus;
            double followMultiplier = (event.getCreatedBy() != null && followedIds.contains(event.getCreatedBy().getId())) ? 1.5 : 1.0;
            
            item.setEngagementScore(baseScore * followMultiplier);

            return item;
        }).collect(Collectors.toList());
    }

    /**
     * Calculate recency score (newer items get higher scores)
     * Rapid decay to ensure old posts don't stay trending forever
     */
    private double calculateRecencyScore(LocalDateTime createdAt) {
        long hoursSinceCreation = Math.abs(ChronoUnit.HOURS.between(createdAt, LocalDateTime.now()));

        // Exponential decay: starts at 20.0, halves approximately every 7 hours
        return 20.0 * Math.exp(-0.1 * hoursSinceCreation);
    }

    /**
     * Get feed statistics
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "feedStats", unless = "#result == null")
    public FeedStats getFeedStats() {
        long totalPosts = postRepository.count();
        long totalEvents = eventRepository.countByStatusAndDateAfter(EventStatus.PUBLISHED, LocalDateTime.now());
        long paidEvents = eventRepository.countByPaidTrue();

        return new FeedStats(totalPosts, totalEvents, paidEvents);
    }
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedStats {
        private long totalPosts;
        private long totalEvents;
        private long paidEvents;
    }
}
