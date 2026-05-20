package com.campussync.backend.Service;

import com.campussync.backend.Dto.*;
import com.campussync.backend.Exception.BadRequestException;
import com.campussync.backend.Exception.ConflictException;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Exception.NotFoundException;
import com.campussync.backend.Model.*;
import com.campussync.backend.Repository.SocietyMembershipRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for society membership request lifecycle.
 *
 * "Society" is modelled as a logical group owned by a User (the society owner/creator).
 * societyId == the owner User's ID for MVP — this avoids needing a separate Society table
 * while keeping the API consistent with what the prompt specifies.
 */
@Service
@RequiredArgsConstructor
public class SocietyMembershipService {

    private final SocietyMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ──────────────────────────────────────────────────────────────────────────
    //  USER ACTIONS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Send a join request to a society.
     * Rejects duplicate PENDING or ACCEPTED memberships.
     */
    @Transactional
    public SocietyMembershipResponse requestJoin(Long societyId, CreateSocietyJoinRequest request) {
        User currentUser = getCurrentUser();

        // Validate society owner exists
        User societyOwner = userRepository.findById(societyId)
                .orElseThrow(() -> new NotFoundException("Society not found"));

        if (currentUser.getId().equals(societyId)) {
            throw new BadRequestException("You cannot send a join request to your own society");
        }

        if (membershipRepository.existsActiveOrPending(societyId, currentUser.getId())) {
            throw new ConflictException("You already have a pending or accepted membership in this society");
        }

        SocietyMembership membership = SocietyMembership.builder()
                .societyId(societyId)
                .user(currentUser)
                .status(SocietyMembershipStatus.PENDING)
                .message(request.getMessage())
                .requestedAt(LocalDateTime.now())
                .build();

        membership = membershipRepository.save(membership);

        // Notify society owner
        notificationService.notifySocietyJoinRequest(societyOwner, currentUser, societyId, membership.getId());

        return toResponse(membership);
    }

    /**
     * Cancel the current user's PENDING join request.
     */
    @Transactional
    public void cancelMyRequest(Long societyId) {
        User currentUser = getCurrentUser();

        SocietyMembership membership = membershipRepository
                .findBySocietyIdAndUserIdAndStatus(societyId, currentUser.getId(), SocietyMembershipStatus.PENDING)
                .orElseThrow(() -> new NotFoundException("No pending join request found for this society"));

        membershipRepository.delete(membership);
    }

    /**
     * Get the current user's membership status for a society.
     */
    @Transactional(readOnly = true)
    public MembershipStatusResponse getMyMembershipStatus(Long societyId) {
        User currentUser = getCurrentUser();

        MembershipStatusResponse response = new MembershipStatusResponse();
        response.setSocietyId(societyId);
        response.setUserId(currentUser.getId());

        membershipRepository.findBySocietyIdAndUserId(societyId, currentUser.getId())
                .ifPresentOrElse(m -> {
                    response.setStatus(m.getStatus().name());
                    response.setMessage(m.getMessage());
                    response.setRejectionReason(m.getRejectionReason());
                    response.setRequestId(m.getId());
                    response.setRequestedAt(m.getRequestedAt());
                    response.setReviewedAt(m.getReviewedAt());
                    response.setMember(m.getStatus() == SocietyMembershipStatus.ACCEPTED);
                }, () -> response.setStatus("NONE"));

        return response;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  SOCIETY OWNER / ADMIN ACTIONS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * List all join requests for a society (owner/admin only).
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<SocietyMembershipResponse> listJoinRequests(Long societyId,
                                                                          SocietyMembershipStatus status,
                                                                          int page, int size) {
        assertCanManageSociety(societyId);
        Pageable pageable = PageRequest.of(page, size);
        Page<SocietyMembership> resultPage = (status != null)
                ? membershipRepository.findBySocietyIdAndStatus(societyId, status, pageable)
                : membershipRepository.findBySocietyId(societyId, pageable);

        List<SocietyMembershipResponse> content = resultPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.isFirst(),
                resultPage.isLast(),
                resultPage.isEmpty()
        );
    }

    /**
     * Accept a join request (owner/admin only).
     */
    @Transactional
    public SocietyMembershipResponse acceptRequest(Long societyId, Long requestId) {
        User reviewer = assertCanManageSociety(societyId);

        SocietyMembership membership = membershipRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Join request not found"));

        if (!membership.getSocietyId().equals(societyId)) {
            throw new ForbiddenOperationException("This request does not belong to this society");
        }
        if (membership.getStatus() != SocietyMembershipStatus.PENDING) {
            throw new ConflictException("Only PENDING requests can be accepted (current: " + membership.getStatus() + ")");
        }

        membership.setStatus(SocietyMembershipStatus.ACCEPTED);
        membership.setReviewedAt(LocalDateTime.now());
        membership.setReviewedBy(reviewer.getId());
        membership = membershipRepository.save(membership);

        // Notify the requesting user
        notificationService.notifySocietyJoinAccepted(membership.getUser(), reviewer, societyId);

        return toResponse(membership);
    }

    /**
     * Reject a join request (owner/admin only).
     */
    @Transactional
    public SocietyMembershipResponse rejectRequest(Long societyId, Long requestId,
                                                     RejectSocietyJoinRequest request) {
        User reviewer = assertCanManageSociety(societyId);

        SocietyMembership membership = membershipRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Join request not found"));

        if (!membership.getSocietyId().equals(societyId)) {
            throw new ForbiddenOperationException("This request does not belong to this society");
        }
        if (membership.getStatus() != SocietyMembershipStatus.PENDING) {
            throw new ConflictException("Only PENDING requests can be rejected (current: " + membership.getStatus() + ")");
        }

        membership.setStatus(SocietyMembershipStatus.REJECTED);
        membership.setRejectionReason(request.getRejectionReason());
        membership.setReviewedAt(LocalDateTime.now());
        membership.setReviewedBy(reviewer.getId());
        membership = membershipRepository.save(membership);

        // Notify the requesting user
        notificationService.notifySocietyJoinRejected(membership.getUser(), reviewer, societyId,
                request.getRejectionReason());

        return toResponse(membership);
    }

    /**
     * List all accepted members of a society (public).
     */
    @Transactional(readOnly = true)
    public List<SocietyMemberResponse> listMembers(Long societyId) {
        // Validate society exists
        userRepository.findById(societyId)
                .orElseThrow(() -> new NotFoundException("Society not found"));

        return membershipRepository.findAcceptedMembers(societyId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Ensures the current user is the society owner or an ADMIN.
     */
    private User assertCanManageSociety(Long societyId) {
        User currentUser = getCurrentUser();
        boolean isOwner = currentUser.getId().equals(societyId);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("Only the society owner or an admin can perform this action");
        }
        return currentUser;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private SocietyMembershipResponse toResponse(SocietyMembership m) {
        SocietyMembershipResponse r = new SocietyMembershipResponse();
        r.setId(m.getId());
        r.setSocietyId(m.getSocietyId());
        r.setUserId(m.getUser().getId());
        r.setUserName(m.getUser().getName());
        r.setUserEmail(m.getUser().getEmail());
        r.setUserProfilePicture(m.getUser().getProfilePictureUrl());
        r.setStatus(m.getStatus().name());
        r.setMessage(m.getMessage());
        r.setRejectionReason(m.getRejectionReason());
        r.setRequestedAt(m.getRequestedAt());
        r.setReviewedAt(m.getReviewedAt());
        r.setReviewedBy(m.getReviewedBy());
        r.setCreatedAt(m.getCreatedAt());
        r.setUpdatedAt(m.getUpdatedAt());
        return r;
    }

    private SocietyMemberResponse toMemberResponse(SocietyMembership m) {
        SocietyMemberResponse r = new SocietyMemberResponse();
        User u = m.getUser();
        r.setUserId(u.getId());
        r.setName(u.getName());
        r.setEmail(u.getEmail());
        r.setProfilePicture(u.getProfilePictureUrl());
        r.setRole(u.getRole() != null ? u.getRole().name() : null);
        r.setMemberSince(m.getReviewedAt());
        return r;
    }
}
