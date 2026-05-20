package com.campussync.backend.Repository;

import com.campussync.backend.Model.EventRegistrationField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRegistrationFieldRepository extends JpaRepository<EventRegistrationField, Long> {

    List<EventRegistrationField> findByEventIdOrderByDisplayOrderAsc(Long eventId);

    void deleteByEventId(Long eventId);

    boolean existsByEventIdAndFieldKey(Long eventId, String fieldKey);
}
