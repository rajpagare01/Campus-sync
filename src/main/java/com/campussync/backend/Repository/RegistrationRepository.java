package com.campussync.backend.Repository;

import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    // Bug #6 Fix: Check for active (non-cancelled) registration only
    boolean existsByUserIdAndEventIdAndStatusNot(Long userId, Long eventId, RegistrationStatus status);
    Optional<Registration> findByUserIdAndEventId(Long userId, Long eventId);
    List<Registration> findByUserId(Long userId);
    List<Registration> findByEventId(Long eventId);
    List<Registration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);
    List<Registration> findByEventIdAndStatusNot(Long eventId, RegistrationStatus status);
    Optional<Registration> findByQrCode(String qrCode);
    void deleteByEventId(Long eventId);
    long countByEventId(Long eventId);
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
    long countByUserId(Long userId);

    @Query("""
            SELECT r FROM Registration r
            WHERE r.event.id = :eventId
              AND (:paid IS NULL OR r.paymentRequired = :paid)
              AND (:attended IS NULL OR 
                   (:attended = true AND (r.attended = true OR r.checkedInAt IS NOT NULL)) OR
                   (:attended = false AND (r.attended = false OR r.attended IS NULL) AND r.checkedInAt IS NULL))
            ORDER BY r.createdAt DESC
            """)
    Page<Registration> findParticipantsByEventId(@Param("eventId") Long eventId,
                                                 @Param("paid") Boolean paid,
                                                 @Param("attended") Boolean attended,
                                                 Pageable pageable);
}
