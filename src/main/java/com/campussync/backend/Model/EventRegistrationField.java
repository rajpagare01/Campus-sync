package com.campussync.backend.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines a dynamic field that event organizers can add to their event registration form.
 * type drives validation and UI rendering on the frontend.
 * options is stored as JSON for SELECT / MULTI_SELECT types.
 */
@Entity
@Table(
    name = "event_registration_fields",
    indexes = {
        @Index(name = "idx_erf_event", columnList = "event_id"),
        @Index(name = "idx_erf_event_order", columnList = "event_id, displayOrder")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistrationField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 255)
    private String label;

    /**
     * Unique machine key per event (used to reference field in answers).
     */
    @Column(nullable = false, length = 100)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RegistrationFieldType type = RegistrationFieldType.TEXT;

    @Column(nullable = false)
    private boolean required = false;

    /**
     * Allowed options for SELECT / MULTI_SELECT types, stored as JSON array string.
     * Example: ["Option A","Option B","Option C"]
     */
    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(length = 255)
    private String placeholder;

    @Column(nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
