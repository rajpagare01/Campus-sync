package com.campussync.backend.Dto;

public record NotificationJobMessage(
        NotificationDTO notification,
        Long userId,
        String email
) {
}
