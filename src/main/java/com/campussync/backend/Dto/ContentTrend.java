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
public class ContentTrend {
    @JsonProperty("trend_id")
    private String trendId;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("trend_name")
    private String trendName;

    @JsonProperty("occurrence_count")
    private Long occurrenceCount;

    @JsonProperty("trend_score")
    private Double trendScore;

    @JsonProperty("posts_count")
    private Long postsCount;

    @JsonProperty("engagement_sum")
    private Long engagementSum;

    @JsonProperty("average_engagement")
    private Double averageEngagement;

    @JsonProperty("growth_rate")
    private Double growthRate;

    @JsonProperty("is_trending")
    private Boolean isTrending;

    @JsonProperty("trend_rank")
    private Integer trendRank;

    @JsonProperty("first_seen")
    private LocalDateTime firstSeen;

    @JsonProperty("last_seen")
    private LocalDateTime lastSeen;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
}
