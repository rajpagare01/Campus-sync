package com.campussync.backend.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EventRegistrationAnswerRequest {

    @NotNull(message = "Field ID is required")
    private Long fieldId;

    @Size(max = 100, message = "Field key must be at most 100 characters")
    private String fieldKey;

    private String answerValue;
}
