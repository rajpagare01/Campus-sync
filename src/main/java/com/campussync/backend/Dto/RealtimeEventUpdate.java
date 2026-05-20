package com.campussync.backend.Dto;

import com.campussync.backend.Model.EventStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RealtimeEventUpdate {
    private Long eventId;
    private String title;
    private String action;
    private String message;
    private EventStatus status;
    private LocalDateTime timestamp;
}
