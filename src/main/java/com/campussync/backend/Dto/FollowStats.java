package com.campussync.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowStats {
    private long followersCount;
    private long followingCount;
    private boolean isFollowing; // Whether current user follows this user
    private boolean isFollowedBy; // Whether this user follows current user
    private boolean isMutual; // Whether it's a mutual follow
}
