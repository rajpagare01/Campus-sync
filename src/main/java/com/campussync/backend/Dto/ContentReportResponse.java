package com.campussync.backend.Dto;

import com.campussync.backend.Model.ReportStatus;
import com.campussync.backend.Model.ReportTargetType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentReportResponse {
    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private String details;
    private ReportStatus status;
    private Long reporterId;
    private String reporterName;
    private Long reviewerId;
    private String reviewerName;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
