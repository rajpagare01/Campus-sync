package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SocietyMembershipResponse {
    private Long id;
    private Long societyId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userProfilePicture;
    private String status;
    private String message;
    private String rejectionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
