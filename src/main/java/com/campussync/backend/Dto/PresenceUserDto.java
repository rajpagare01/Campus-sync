package com.campussync.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresenceUserDto {
    private Long userId;
    private String email;
}
