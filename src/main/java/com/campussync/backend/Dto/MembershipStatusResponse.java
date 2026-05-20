package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MembershipStatusResponse {
    private Long societyId;
    private Long userId;
    /** NONE | PENDING | ACCEPTED | REJECTED */
    private String status;
    private String message;
    private String rejectionReason;
    private Long requestId;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private boolean isMember;
}
