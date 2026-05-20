package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRegistrationResponse {

    private Long registrationId;
    private Long userId;

    private Long eventId;

    private String eventTitle;
    private String eventVenue;

    private String status;
    private String paymentStatus;
    private boolean paymentRequired;
    private boolean attended;
    private LocalDateTime checkedInAt;
    private String qrCode;
}
