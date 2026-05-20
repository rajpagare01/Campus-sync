package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private Long postId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // For threaded comments
    private Long parentCommentId;
    private List<CommentResponse> replies;
    private int replyCount;
}
