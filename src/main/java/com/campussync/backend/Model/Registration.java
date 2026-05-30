package com.campussync.backend.Model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration", indexes = {
    @Index(name = "idx_registration_event_status", columnList = "event_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many registrations → one user
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    // Many registrations → one event
    @ManyToOne(fetch = FetchType.LAZY)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;
    private Boolean paymentRequired = false;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RegistrationPaymentStatus paymentStatus = RegistrationPaymentStatus.NOT_REQUIRED;
    private Long paymentOrderId;
    private Boolean attended = false;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AttendanceStatus attendanceStatus = AttendanceStatus.NOT_CHECKED_IN;
    private LocalDateTime checkedInAt;
    @Column(length = 1024)
    private String qrCode;

    /** Feature 4: Tracks when the participant first downloaded their certificate */
    private LocalDateTime certificateDownloadedAt;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RegistrationStatus.REGISTERED;
        }
        if (paymentStatus == null) {
            paymentStatus = RegistrationPaymentStatus.NOT_REQUIRED;
        }
        if (attendanceStatus == null) {
            attendanceStatus = AttendanceStatus.NOT_CHECKED_IN;
        }
        if (attended == null) {
            attended = false;
        }
    }

    public boolean isRegistered() {
        return status == RegistrationStatus.REGISTERED;
    }

    public boolean isCancelled() {
        return status == RegistrationStatus.CANCELLED;
    }

    public boolean isPaymentPending() {
        return status == RegistrationStatus.PAYMENT_PENDING;
    }

    public void markRegistered() {
        this.status = RegistrationStatus.REGISTERED;
    }

    public void markPaymentPending() {
        this.status = RegistrationStatus.PAYMENT_PENDING;
        this.paymentRequired = true;
        this.paymentStatus = RegistrationPaymentStatus.PENDING;
    }

    public void markCancelled() {
        this.status = RegistrationStatus.CANCELLED;
    }

    public void markPaymentNotRequired() {
        this.paymentRequired = false;
        this.paymentStatus = RegistrationPaymentStatus.NOT_REQUIRED;
    }

    public void markPaymentFailed() {
        this.paymentStatus = RegistrationPaymentStatus.FAILED;
    }

    public void markPaymentPaid() {
        this.paymentStatus = RegistrationPaymentStatus.PAID;
    }

    public void markPaymentRefunded() {
        this.paymentStatus = RegistrationPaymentStatus.REFUNDED;
    }

    public void markPaymentCancelled() {
        this.paymentStatus = RegistrationPaymentStatus.CANCELLED;
    }

    public void markCheckedIn(LocalDateTime when) {
        this.attended = true;
        this.attendanceStatus = AttendanceStatus.CHECKED_IN;
        this.checkedInAt = when;
    }
}
