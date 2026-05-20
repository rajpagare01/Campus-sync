package com.campussync.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowResponse {
    private Long id;
    private Long followerId;
    private String followerName;
    private Long followingId;
    private String followingName;
    private LocalDateTime createdAt;
    private boolean isMutual; // Whether the follow is mutual
}
