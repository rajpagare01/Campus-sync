package com.campussync.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatSource {
    private String type;
    private Long id;
    private String title;
    private String subtitle;
    private String venue;
    private LocalDateTime date;
    private boolean registered;
}
