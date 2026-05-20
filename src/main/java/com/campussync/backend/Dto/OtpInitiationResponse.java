package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class OtpInitiationResponse {
    private String message;
    private String email;
}
