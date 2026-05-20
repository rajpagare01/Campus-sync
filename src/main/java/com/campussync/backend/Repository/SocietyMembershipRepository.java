package com.campussync.backend.Repository;

import com.campussync.backend.Model.SocietyMembership;
import com.campussync.backend.Model.SocietyMembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocietyMembershipRepository extends JpaRepository<SocietyMembership, Long> {

    Optional<SocietyMembership> findBySocietyIdAndUserId(Long societyId, Long userId);

    boolean existsBySocietyIdAndUserIdAndStatus(Long societyId, Long userId, SocietyMembershipStatus status);

    /**
     * Check if any non-terminal (PENDING or ACCEPTED) membership exists.
     */
    @Query("SELECT COUNT(m) > 0 FROM SocietyMembership m WHERE m.societyId = :societyId AND m.user.id = :userId AND m.status IN ('PENDING', 'ACCEPTED')")
    boolean existsActiveOrPending(@Param("societyId") Long societyId, @Param("userId") Long userId);

    Page<SocietyMembership> findBySocietyIdAndStatus(Long societyId, SocietyMembershipStatus status, Pageable pageable);

    Page<SocietyMembership> findBySocietyId(Long societyId, Pageable pageable);

    List<SocietyMembership> findBySocietyIdAndStatus(Long societyId, SocietyMembershipStatus status);

    @Query("SELECT m FROM SocietyMembership m JOIN FETCH m.user WHERE m.societyId = :societyId AND m.status = 'ACCEPTED'")
    List<SocietyMembership> findAcceptedMembers(@Param("societyId") Long societyId);

    Optional<SocietyMembership> findBySocietyIdAndUserIdAndStatus(Long societyId, Long userId, SocietyMembershipStatus status);
}
