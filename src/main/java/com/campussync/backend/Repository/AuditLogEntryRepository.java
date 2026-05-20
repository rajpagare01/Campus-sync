package com.campussync.backend.Repository;

import com.campussync.backend.Model.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, Long> {
    Page<AuditLogEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
