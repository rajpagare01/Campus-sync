package com.campussync.backend.Dto;

import com.campussync.backend.Model.EventStatus;
import lombok.Data;

@Data
public class EventAnalyticsResponse {
    private Long eventId;
    private String title;
    private EventStatus status;
    private long views;
    private long totalRegistrations;
    private long activeRegistrations;
    private long cancelledRegistrations;
    private double registrationConversionRate;
    private double engagementScore;
}
