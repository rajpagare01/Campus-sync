package com.campussync.backend.Dto;

import com.campussync.backend.Model.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContentReportRequest {
    @NotNull
    private ReportTargetType targetType;

    @NotNull
    private Long targetId;

    @NotBlank
    private String reason;

    private String details;
}
