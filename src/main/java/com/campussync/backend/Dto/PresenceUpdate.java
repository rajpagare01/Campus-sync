package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PresenceUpdate {
    private Long userId;
    private String email;
    private boolean online;
    private int onlineCount;
    private LocalDateTime timestamp;
}
