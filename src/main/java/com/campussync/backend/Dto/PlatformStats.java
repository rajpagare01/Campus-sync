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
public class PlatformStats {
    @JsonProperty("total_users")
    private Long totalUsers;

    @JsonProperty("active_users_7d")
    private Long activeUsers7d;

    @JsonProperty("active_users_30d")
    private Long activeUsers30d;

    @JsonProperty("daily_active_users")
    private Long dailyActiveUsers;

    @JsonProperty("total_posts")
    private Long totalPosts;

    @JsonProperty("posts_today")
    private Long postsToday;

    @JsonProperty("posts_this_month")
    private Long postsThisMonth;

    @JsonProperty("total_events")
    private Long totalEvents;

    @JsonProperty("active_events")
    private Long activeEvents;

    @JsonProperty("total_comments")
    private Long totalComments;

    @JsonProperty("comments_today")
    private Long commentsToday;

    @JsonProperty("total_likes")
    private Long totalLikes;

    @JsonProperty("likes_today")
    private Long likesToday;

    @JsonProperty("average_posts_per_user")
    private Double averagePostsPerUser;

    @JsonProperty("average_engagement_rate")
    private Double averageEngagementRate;

    @JsonProperty("user_growth_rate")
    private Double userGrowthRate;

    @JsonProperty("engagement_trend")
    private String engagementTrend;

    @JsonProperty("platform_health_score")
    private Double platformHealthScore;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
}
