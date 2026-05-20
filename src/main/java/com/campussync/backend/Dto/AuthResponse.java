package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class AuthResponse {
    private UserDto user;
    private String accessToken;
    private String refreshToken;
    private String message;
}
