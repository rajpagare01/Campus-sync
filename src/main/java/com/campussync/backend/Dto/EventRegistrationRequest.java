package com.campussync.backend.Dto;

import lombok.Data;

import java.util.List;

/**
 * Request body for event registration, optionally including answers to dynamic registration fields.
 * Backward compatible: events with no custom fields receive an empty/null answers list.
 */
@Data
public class EventRegistrationRequest {
    private List<EventRegistrationAnswerRequest> answers;
}
