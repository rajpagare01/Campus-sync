package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CertificateEligibilityResponse {
    private Long eventId;
    private Long userId;
    private boolean eligible;
    private String reason; // populated when not eligible
    private boolean certificateEnabled;
    private boolean attended;
    private boolean certificateAvailable;
    private boolean registered;
    private LocalDateTime certificateDownloadedAt;
}
