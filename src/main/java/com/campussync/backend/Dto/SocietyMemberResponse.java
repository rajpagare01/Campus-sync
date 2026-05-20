package com.campussync.backend.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SocietyMemberResponse {
    private Long userId;
    private String name;
    private String email;
    private String profilePicture;
    private String role;
    private LocalDateTime memberSince; // = reviewedAt
}
