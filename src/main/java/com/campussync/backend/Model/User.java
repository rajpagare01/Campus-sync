package com.campussync.backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    @JsonIgnore
    private String email;

    @JsonIgnore
    private String password;

    private boolean isVerified;

    @JsonIgnore
    private String verificationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "\"role\"")
    private Role role;

    private Integer tokenVersion = 0;
    private boolean active = true;
    private LocalDateTime deactivatedAt;
    private String deactivationReason;
    @Column(columnDefinition = "TEXT")
    private String bio;
    private String profilePictureUrl;
    private String department;
    @Column(name = "\"year\"")
    private String year;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
