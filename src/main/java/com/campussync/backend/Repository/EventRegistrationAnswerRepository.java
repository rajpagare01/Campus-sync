package com.campussync.backend.Repository;

import com.campussync.backend.Model.EventRegistrationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationAnswerRepository extends JpaRepository<EventRegistrationAnswer, Long> {

    List<EventRegistrationAnswer> findByRegistrationId(Long registrationId);

    List<EventRegistrationAnswer> findByRegistrationIdIn(List<Long> registrationIds);

    Optional<EventRegistrationAnswer> findByRegistrationIdAndFieldId(Long registrationId, Long fieldId);

    boolean existsByRegistrationIdAndFieldId(Long registrationId, Long fieldId);

    void deleteByRegistrationId(Long registrationId);

    @Query("SELECT a FROM EventRegistrationAnswer a WHERE a.registration.event.id = :eventId")
    List<EventRegistrationAnswer> findByEventId(@Param("eventId") Long eventId);
}
