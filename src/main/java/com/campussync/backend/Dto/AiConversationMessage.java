package com.campussync.backend.Dto;

import com.campussync.backend.Model.AiMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationMessage {
    private Long id;
    private AiMessage.Role role;
    private String content;
    private LocalDateTime createdAt;
}
