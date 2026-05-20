package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RealtimeFeedUpdate {
    private String entityType;
    private Long entityId;
    private String action;
    private LocalDateTime timestamp;
}
