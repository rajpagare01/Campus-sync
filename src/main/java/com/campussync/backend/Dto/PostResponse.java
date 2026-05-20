package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {
    private Long id;
    private String content;
    private String mediaUrl;
    private Long eventId; // Linked event ID
    private String eventTitle; // Linked event title (if any)

    // Author information
    private Long authorId;
    private String authorName;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Engagement metrics
    private int likeCount;
    private int likesCount;
    private int commentCount;
    private int commentsCount;

    // User's interaction status (will be set by service)
    private boolean isLikedByCurrentUser;
    private boolean liked;

    // Author details
    private String authorFirstName;
    private String authorLastName;
    private String authorProfilePicture;
    private String authorProfilePictureUrl;

    // Nested author for modern frontend components
    private AuthorDto author;
}
