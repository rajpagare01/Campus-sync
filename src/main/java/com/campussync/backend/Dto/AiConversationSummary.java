package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConversationSummary {
    private Long conversationId;
    private String title;
    private String lastMessagePreview;
    private long messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
