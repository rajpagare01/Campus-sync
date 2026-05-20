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
public class PostPerformanceMetrics {
    @JsonProperty("post_id")
    private Long postId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("content_preview")
    private String contentPreview;

    @JsonProperty("total_likes")
    private Long totalLikes;

    @JsonProperty("total_comments")
    private Long totalComments;

    @JsonProperty("total_shares")
    private Long totalShares;

    @JsonProperty("total_views")
    private Long totalViews;

    @JsonProperty("engagement_rate")
    private Double engagementRate;

    @JsonProperty("engagement_score")
    private Double engagementScore;

    @JsonProperty("popularity_rank")
    private Integer popularityRank;

    @JsonProperty("post_date")
    private LocalDateTime postDate;

    @JsonProperty("trending")
    private Boolean trending;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
}
