package com.campussync.backend.Repository;

import com.campussync.backend.Model.Report;
import com.campussync.backend.Model.ReportStatus;
import com.campussync.backend.Model.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
            SELECT r FROM Report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
            ORDER BY r.createdAt DESC
            """)
    Page<Report> findForModeration(@Param("status") ReportStatus status,
                                   @Param("targetType") ReportTargetType targetType,
                                   Pageable pageable);

    long countByStatus(ReportStatus status);
}
