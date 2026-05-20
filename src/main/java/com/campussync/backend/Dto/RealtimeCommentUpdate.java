package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RealtimeCommentUpdate {
    private Long postId;
    private Long commentId;
    private Long parentCommentId;
    private String content;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime timestamp;
}
