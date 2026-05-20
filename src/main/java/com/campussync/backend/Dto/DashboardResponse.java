package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class DashboardResponse {

    private long totalUsers;
    private long totalEvents;
    private long totalRegistrations;
    private long paidEvents;
    private long publishedEvents;
    private long cancelledEvents;
    private long totalEventViews;
    private double averageRegistrationsPerEvent;
    private double eventEngagementRate;
}
