package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class AdminUserStatusRequest {
    private boolean active;
    private String reason;
}
