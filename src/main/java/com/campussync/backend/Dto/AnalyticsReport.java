package com.campussync.backend.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReport {
    @JsonProperty("report_id")
    private String reportId;

    @JsonProperty("report_type")
    private String reportType;

    @JsonProperty("period_start")
    private LocalDateTime periodStart;

    @JsonProperty("period_end")
    private LocalDateTime periodEnd;

    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;

    @JsonProperty("generated_by")
    private String generatedBy;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("key_metrics")
    private Object keyMetrics;

    @JsonProperty("insights")
    private List<String> insights;

    @JsonProperty("recommendations")
    private List<String> recommendations;

    @JsonProperty("data_sections")
    private List<Object> dataSections;

    @JsonProperty("format")
    private String format;

    @JsonProperty("filename")
    private String filename;
}
