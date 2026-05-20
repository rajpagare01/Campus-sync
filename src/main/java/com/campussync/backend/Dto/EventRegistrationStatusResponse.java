package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class EventRegistrationStatusResponse {
    private Long eventId;
    private boolean registered;
    private boolean canCancel;
    private Long registrationId;
    private String status;
    private String paymentStatus;
    private boolean paymentRequired;
}
