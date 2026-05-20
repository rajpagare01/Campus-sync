package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventCheckInResponse {
    private Long registrationId;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userName;
    private boolean attended;
    private LocalDateTime checkedInAt;
    private String message;
}
