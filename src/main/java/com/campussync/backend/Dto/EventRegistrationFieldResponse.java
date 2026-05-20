package com.campussync.backend.Dto;

import com.campussync.backend.Model.RegistrationFieldType;
import lombok.Data;

import java.util.List;

@Data
public class EventRegistrationFieldResponse {
    private Long id;
    private Long eventId;
    private String label;
    private String fieldKey;
    private RegistrationFieldType type;
    private boolean required;
    private List<String> options;
    private String placeholder;
    private int displayOrder;
}
