package com.campussync.backend.Dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectSocietyJoinRequest {

    @Size(max = 1024, message = "Rejection reason must be at most 1024 characters")
    private String rejectionReason;
}
