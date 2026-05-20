package com.campussync.backend.Dto;

import com.campussync.backend.Model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModerationReviewRequest {
    @NotNull
    private ReportStatus status;
    private String resolutionNotes;
    private boolean removeContent;
}
