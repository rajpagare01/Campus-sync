package com.campussync.backend.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EventCheckInRequest {
    private String qrCode;

    private Long registrationId;
    private Long userId;
}
