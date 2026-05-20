package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatResponse {
    private Long conversationId;
    private String answer;
    private boolean fallback;
    private LocalDateTime createdAt;
    private List<AiChatSource> sources = new ArrayList<>();
}
