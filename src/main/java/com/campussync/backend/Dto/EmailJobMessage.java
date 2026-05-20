package com.campussync.backend.Dto;

public record EmailJobMessage(
        String to,
        String subject,
        String body
) {
}
