package com.campussync.backend.Dto;

import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String venue;
    private LocalDateTime date;
    private EventType type;
    private String category;
    private Integer maxAttendees;
    private Boolean paid;
    private Double price;
    private String imageUrl;
    private EventStatus status;
    private Long viewsCount;
    private LocalDateTime createdAt;

    // Feature 4: Certificate download eligibility
    private Boolean certificateEnabled;

    // Feature 5: Dynamic registration fields
    private List<EventRegistrationFieldResponse> registrationFields;

    // Creator information
    private AuthorDto author;
    private Long creatorId; // For backward compatibility

    // Compatibility fields
    private Long registrationCount;
    private Long registrationsCount;
    private LocalDateTime eventDate; // match FeedItem
}

