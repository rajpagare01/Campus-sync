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
public class UserEngagementMetrics {
    @JsonProperty("total_users")
    private Long totalUsers;

    @JsonProperty("active_users_7d")
    private Long activeUsers7d;

    @JsonProperty("active_users_30d")
    private Long activeUsers30d;

    @JsonProperty("new_users_today")
    private Long newUsersToday;

    @JsonProperty("new_users_this_month")
    private Long newUsersThisMonth;

    @JsonProperty("engagement_rate")
    private Double engagementRate;

    @JsonProperty("posts_per_user")
    private Double postsPerUser;

    @JsonProperty("comments_per_user")
    private Double commentsPerUser;

    @JsonProperty("likes_per_user")
    private Double likesPerUser;

    @JsonProperty("daily_active_users")
    private Long dailyActiveUsers;

    @JsonProperty("monthly_active_users")
    private Long monthlyActiveUsers;

    @JsonProperty("retention_rate_day7")
    private Double retentionRateDay7;

    @JsonProperty("retention_rate_day30")
    private Double retentionRateDay30;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;

    @JsonProperty("period_start")
    private LocalDateTime periodStart;

    @JsonProperty("period_end")
    private LocalDateTime periodEnd;
}
