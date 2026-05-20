package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class EventRegistrationAnswerResponse {
    private Long fieldId;
    private String fieldKey;
    private String label;
    private String answerValue;
}
