package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedItem {
    private String type; // "POST" or "EVENT"
    private Long id;
    private LocalDateTime createdAt;

    // Common fields
    private String title;
    private String content;
    private String authorName;
    private Long authorId;

    // Post-specific fields
    private String mediaUrl;
    private Integer likeCount;
    private Integer likesCount;
    private Integer commentCount;
    private Integer commentsCount;
    private Boolean isLikedByCurrentUser;
    private Boolean liked;

    // Author details for nested structure compatibility
    private String authorFirstName;
    private String authorLastName;
    private String authorProfilePicture;
    private String profilePictureUrl;

    // Event-specific fields
    private String venue;
    private LocalDateTime eventDate;
    private String eventType;
    private Boolean paid;
    private Double price;
    private String imageUrl;
    private Long registrationCount;

    // Engagement score for sorting
    private Double engagementScore;
}
