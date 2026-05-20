package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiConversationDetailResponse {
    private Long conversationId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AiConversationMessage> messages = new ArrayList<>();
}
