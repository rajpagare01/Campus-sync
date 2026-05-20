package com.campussync.backend.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "follow_relationships",
       uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower; // The user who is following

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following; // The user being followed

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Prevent self-following
    @PrePersist
    @PreUpdate
    private void validateNotSelfFollow() {
        if (follower != null && following != null && follower.getId().equals(following.getId())) {
            throw new IllegalArgumentException("Users cannot follow themselves");
        }
    }
}
