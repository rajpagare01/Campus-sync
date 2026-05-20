package com.campussync.backend.Dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSocietyJoinRequest {

    @Size(max = 1024, message = "Message must be at most 1024 characters")
    private String message;
}
