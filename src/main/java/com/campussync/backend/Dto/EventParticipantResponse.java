package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventParticipantResponse {
    private Long registrationId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String role;
    private String department;
    private String year;
    private String status;
    private boolean paymentRequired;
    private String paymentStatus;
    private boolean attended;
    private String attendanceStatus;
    private LocalDateTime checkedInAt;
    private LocalDateTime registeredAt;

    // Feature 5: Dynamic registration field answers
    private List<EventRegistrationAnswerResponse> answers;
}

