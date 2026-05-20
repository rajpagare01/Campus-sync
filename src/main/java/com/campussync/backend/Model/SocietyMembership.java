package com.campussync.backend.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a user's request to join a society (or society-owned event organizer group).
 * societyId is a logical reference — the "society" is the event creator/org group identified by userId of creator.
 * For MVP: societyId maps to a User (creator/society owner) ID.
 */
@Entity
@Table(
    name = "society_memberships",
    indexes = {
        @Index(name = "idx_sm_society_user", columnList = "societyId, userId", unique = true),
        @Index(name = "idx_sm_society_status", columnList = "societyId, status"),
        @Index(name = "idx_sm_user", columnList = "userId")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocietyMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long societyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SocietyMembershipStatus status = SocietyMembershipStatus.PENDING;

    @Column(length = 1024)
    private String message;

    @Column(length = 1024)
    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime reviewedAt;

    private Long reviewedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SocietyMembershipStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
