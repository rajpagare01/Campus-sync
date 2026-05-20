package com.campussync.backend.Controller;

import com.campussync.backend.Dto.*;
import com.campussync.backend.Model.SocietyMembershipStatus;
import com.campussync.backend.Service.SocietyMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for society membership request lifecycle.
 *
 * Route prefix: /api/societies/{societyId}/...
 *
 * "Society" in this context is the group owned by a user (the society owner).
 * societyId == the User ID of the society owner.
 */
@RestController
@RequestMapping({"/api/societies", "/api/v1/societies"})
@RequiredArgsConstructor
public class SocietyController {

    private final SocietyMembershipService membershipService;

    // ─── User actions ────────────────────────────────────────────────────────

    /**
     * POST /api/societies/{societyId}/join-requests
     * Submit a membership request.
     */
    @PostMapping("/{societyId}/join-requests")
    @PreAuthorize("isAuthenticated()")
    public SocietyMembershipResponse requestJoin(@PathVariable Long societyId,
                                                  @Valid @RequestBody(required = false) CreateSocietyJoinRequest request) {
        return membershipService.requestJoin(societyId, request != null ? request : new CreateSocietyJoinRequest());
    }

    /**
     * DELETE /api/societies/{societyId}/join-requests/me
     * Cancel own pending request.
     */
    @DeleteMapping("/{societyId}/join-requests/me")
    @PreAuthorize("isAuthenticated()")
    public void cancelMyRequest(@PathVariable Long societyId) {
        membershipService.cancelMyRequest(societyId);
    }

    /**
     * GET /api/societies/{societyId}/membership-status
     * Get current user's membership status for this society.
     */
    @GetMapping("/{societyId}/membership-status")
    @PreAuthorize("isAuthenticated()")
    public MembershipStatusResponse getMyMembershipStatus(@PathVariable Long societyId) {
        return membershipService.getMyMembershipStatus(societyId);
    }

    // ─── Society owner / Admin actions ───────────────────────────────────────

    /**
     * GET /api/societies/{societyId}/join-requests
     * List all (or filtered) join requests for a society (owner/admin).
     */
    @GetMapping("/{societyId}/join-requests")
    @PreAuthorize("isAuthenticated()")
    public PaginatedResponse<SocietyMembershipResponse> listJoinRequests(
            @PathVariable Long societyId,
            @RequestParam(required = false) SocietyMembershipStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return membershipService.listJoinRequests(societyId, status, page, size);
    }

    /**
     * POST /api/societies/{societyId}/join-requests/{requestId}/accept
     * Accept a pending join request.
     */
    @PostMapping("/{societyId}/join-requests/{requestId}/accept")
    @PreAuthorize("isAuthenticated()")
    public SocietyMembershipResponse acceptRequest(@PathVariable Long societyId,
                                                    @PathVariable Long requestId) {
        return membershipService.acceptRequest(societyId, requestId);
    }

    /**
     * POST /api/societies/{societyId}/join-requests/{requestId}/reject
     * Reject a pending join request with an optional reason.
     */
    @PostMapping("/{societyId}/join-requests/{requestId}/reject")
    @PreAuthorize("isAuthenticated()")
    public SocietyMembershipResponse rejectRequest(@PathVariable Long societyId,
                                                    @PathVariable Long requestId,
                                                    @Valid @RequestBody(required = false) RejectSocietyJoinRequest request) {
        return membershipService.rejectRequest(societyId, requestId,
                request != null ? request : new RejectSocietyJoinRequest());
    }

    // ─── Public ──────────────────────────────────────────────────────────────

    /**
     * GET /api/societies/{societyId}/members
     * List accepted members (publicly accessible).
     */
    @GetMapping("/{societyId}/members")
    public List<SocietyMemberResponse> listMembers(@PathVariable Long societyId) {
        return membershipService.listMembers(societyId);
    }
}
