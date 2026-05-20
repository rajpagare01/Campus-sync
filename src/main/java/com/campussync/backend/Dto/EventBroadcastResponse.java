package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventBroadcastResponse {
    private Long eventId;
    private String eventTitle;
    private int recipients;
    private String message;
    private LocalDateTime sentAt;
}
