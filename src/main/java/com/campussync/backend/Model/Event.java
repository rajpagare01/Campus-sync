package com.campussync.backend.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String venue;
    private LocalDateTime date;
    @Enumerated(EnumType.STRING)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean paid = false;
    private double price;
    private String imageUrl;
    private String category;
    private Integer maxAttendees;
    @Column(nullable = false)
    private Long viewsCount = 0L;

    /** Feature 4: Certificate download eligibility flag */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean certificateEnabled = false;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createdAt;

    // Bug #9 Fix: Ensure createdAt is never null for Event entities.
    // Without this, EventService.createEvent() never sets createdAt, resulting
    // in null values that cause NPEs in UserService.getUserActivity().
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
