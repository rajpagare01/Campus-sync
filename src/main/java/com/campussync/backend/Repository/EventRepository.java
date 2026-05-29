package com.campussync.backend.Repository;

import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import com.campussync.backend.Model.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    long countByPaidTrue();
    long countByStatus(EventStatus status);
    @Query("SELECT COALESCE(SUM(e.viewsCount), 0) FROM Event e")
    long sumAllViews();
    List<Event> findByStatusOrderByDateAsc(EventStatus status);

    // 🆕 Pagination support for published events
    Page<Event> findByStatusOrderByDateAsc(EventStatus status, Pageable pageable);

    // 🆕 Efficient query for feed: Only upcoming events, paginated
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.date > :now ORDER BY e.date ASC")
    Page<Event> findUpcomingEvents(@Param("status") EventStatus status, @Param("now") LocalDateTime now, Pageable pageable);

    // 🆕 Pagination support for search
    @Query("""
            SELECT e FROM Event e
            WHERE (:keyword IS NULL OR
                   LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:type IS NULL OR e.type = :type)
              AND (:status IS NULL OR e.status = :status)
            ORDER BY e.date ASC
            """)
    Page<Event> searchEvents(@Param("keyword") String keyword,
                            @Param("type") EventType type,
                            @Param("status") EventStatus status,
                            Pageable pageable);

    // 🆕 Non-paginated search for legacy support
    @Query("""
            SELECT e FROM Event e
            WHERE (:keyword IS NULL OR
                   LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:type IS NULL OR e.type = :type)
              AND (:status IS NULL OR e.status = :status)
            ORDER BY e.date ASC
            """)
    List<Event> searchEvents(@Param("keyword") String keyword,
                             @Param("type") EventType type,
                             @Param("status") EventStatus status);

    //  Find events by creator with pagination
    Page<Event> findByCreatedByIdOrderByDateDesc(Long creatorId, Pageable pageable);

    //  Count events by creator
    long countByCreatedById(Long creatorId);

    // Analytics queries
    @Query("SELECT COUNT(DISTINCT r.user.id) FROM Registration r WHERE r.event.id = :eventId")
    long countEventAttendees(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(DISTINCT r.user.id) FROM Registration r WHERE r.event.id = :eventId AND r.status <> :cancelledStatus")
    long countEventActualAttendees(@Param("eventId") Long eventId,
                                   @Param("cancelledStatus") RegistrationStatus cancelledStatus);

    long countByDateAfter(LocalDateTime date);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Page<Event> findByVenueContainsIgnoreCaseOrderByDateDesc(String venue, Pageable pageable);
    
    Page<Event> findByType(EventType type, Pageable pageable);

}
