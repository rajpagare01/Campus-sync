package com.campussync.backend.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRetentionMetrics {
    @JsonProperty("cohort_date")
    private LocalDateTime cohortDate;

    @JsonProperty("cohort_size")
    private Long cohortSize;

    @JsonProperty("retained_day1")
    private Long retainedDay1;

    @JsonProperty("retained_day7")
    private Long retainedDay7;

    @JsonProperty("retained_day30")
    private Long retainedDay30;

    @JsonProperty("retained_day90")
    private Long retainedDay90;

    @JsonProperty("retention_rate_day1")
    private Double retentionRateDay1;

    @JsonProperty("retention_rate_day7")
    private Double retentionRateDay7;

    @JsonProperty("retention_rate_day30")
    private Double retentionRateDay30;

    @JsonProperty("retention_rate_day90")
    private Double retentionRateDay90;

    @JsonProperty("churn_rate")
    private Double churnRate;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
}
