package com.campussync.backend.Dto;

import com.campussync.backend.Model.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserSummary {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean verified;
    private boolean active;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deactivatedAt;
    private String deactivationReason;
}
