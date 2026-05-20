package com.campussync.backend.Dto;

import com.campussync.backend.Model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    
    // Profile information
    private String bio;
    private String profilePictureUrl;
    private String department;
    private String year;
    
    // Account information
    private boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Activity statistics (populated by service)
    private int postCount;
    private int postsCount;
    private int eventCount;
    private int eventsCount;
    private int likeCount;
    private int likesCount;
    private int commentCount;
    private int commentsCount;

    // Follow statistics (populated by service)
    private int followersCount;
    private int followingCount;
    private boolean isFollowing; // if current user follows this profile user
    private boolean isFollowedBy; // if this profile user follows current user
    private boolean isMutual;
}
