package com.campussync.backend.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUserStats {
    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("new_users")
    private Long newUsers;

    @JsonProperty("daily_active_users")
    private Long dailyActiveUsers;

    @JsonProperty("total_posts")
    private Long totalPosts;

    @JsonProperty("total_comments")
    private Long totalComments;

    @JsonProperty("total_likes")
    private Long totalLikes;

    @JsonProperty("total_engagement")
    private Long totalEngagement;

    @JsonProperty("engagement_rate")
    private Double engagementRate;

    @JsonProperty("posts_created")
    private Long postsCreated;

    @JsonProperty("comments_created")
    private Long commentsCreated;

    @JsonProperty("likes_created")
    private Long likesCreated;

    @JsonProperty("new_events")
    private Long newEvents;

    @JsonProperty("event_registrations")
    private Long eventRegistrations;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
}
