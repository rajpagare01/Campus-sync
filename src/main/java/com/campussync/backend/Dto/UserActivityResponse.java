package com.campussync.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivityResponse {
    private Long id;
    private String activityType; // "POST", "LIKE", "COMMENT", "REGISTRATION"
    private String description;
    private LocalDateTime timestamp;
    
    // Additional context based on activity type
    private Long relatedId; // Post ID, Event ID, etc.
    private String relatedTitle;
}
