package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RealtimeLikeUpdate {
    private Long postId;
    private Long userId;
    private String userName;
    private boolean liked;
    private int likeCount;
    private LocalDateTime timestamp;
}
