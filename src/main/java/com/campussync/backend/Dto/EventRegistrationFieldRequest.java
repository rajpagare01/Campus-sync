package com.campussync.backend.Dto;

import com.campussync.backend.Model.RegistrationFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EventRegistrationFieldRequest {

    @NotBlank(message = "Field label is required")
    @Size(max = 255, message = "Label must be at most 255 characters")
    private String label;

    @NotBlank(message = "Field key is required")
    @Size(max = 100, message = "Field key must be at most 100 characters")
    private String fieldKey;

    @NotNull(message = "Field type is required")
    private RegistrationFieldType type;

    private boolean required = false;

    /**
     * Allowed options for SELECT / MULTI_SELECT fields.
     */
    private List<String> options;

    @Size(max = 255, message = "Placeholder must be at most 255 characters")
    private String placeholder;

    private int displayOrder = 0;
}
