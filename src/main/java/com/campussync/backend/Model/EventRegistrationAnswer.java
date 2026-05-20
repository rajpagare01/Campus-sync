package com.campussync.backend.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores a single user's answer to one EventRegistrationField for a specific Registration.
 * labelSnapshot captures the label at answer-time so CSV exports remain stable even if the field label changes later.
 */
@Entity
@Table(
    name = "event_registration_answers",
    indexes = {
        @Index(name = "idx_era_registration", columnList = "registration_id"),
        @Index(name = "idx_era_field", columnList = "field_id"),
        @Index(name = "idx_era_reg_field", columnList = "registration_id, field_id", unique = true)
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistrationAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private EventRegistrationField field;

    @Column(nullable = false, length = 100)
    private String fieldKey;

    /**
     * Snapshot of the field label at submission time to ensure export stability.
     */
    @Column(nullable = false, length = 255)
    private String labelSnapshot;

    @Column(columnDefinition = "TEXT")
    private String answerValue;

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
