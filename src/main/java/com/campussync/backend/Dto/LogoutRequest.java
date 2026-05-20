package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
    private boolean logoutAll;
}
