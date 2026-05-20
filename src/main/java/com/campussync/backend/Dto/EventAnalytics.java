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
public class EventAnalytics {
    @JsonProperty("event_id")
    private Long eventId;

    @JsonProperty("event_title")
    private String eventTitle;

    @JsonProperty("event_category")
    private String eventCategory;

    @JsonProperty("total_registrations")
    private Long totalRegistrations;

    @JsonProperty("actual_attendees")
    private Long actualAttendees;

    @JsonProperty("attendance_rate")
    private Double attendanceRate;

    @JsonProperty("total_posts")
    private Long totalPosts;

    @JsonProperty("total_comments")
    private Long totalComments;

    @JsonProperty("total_likes")
    private Long totalLikes;

    @JsonProperty("engagement_rate")
    private Double engagementRate;

    @JsonProperty("average_likes_per_post")
    private Double averageLikesPerPost;

    @JsonProperty("average_comments_per_post")
    private Double averageCommentsPerPost;

    @JsonProperty("popularity_rank")
    private Integer popularityRank;

    @JsonProperty("event_date")
    private LocalDateTime eventDate;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
}
