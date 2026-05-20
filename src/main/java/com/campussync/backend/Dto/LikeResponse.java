package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LikeResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long postId;
    private LocalDateTime createdAt;
}
