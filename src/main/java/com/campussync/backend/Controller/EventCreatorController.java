package com.campussync.backend.Controller;

import com.campussync.backend.Dto.EventBroadcastRequest;
import com.campussync.backend.Dto.EventBroadcastResponse;
import com.campussync.backend.Dto.EventCheckInRequest;
import com.campussync.backend.Dto.EventCheckInResponse;
import com.campussync.backend.Dto.EventParticipantResponse;
import com.campussync.backend.Dto.EventRegistrationFieldRequest;
import com.campussync.backend.Dto.EventRegistrationFieldResponse;
import com.campussync.backend.Dto.FeedbackRequest;
import com.campussync.backend.Dto.FeedbackResponse;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Service.DynamicRegistrationFieldService;
import com.campussync.backend.Service.EventParticipantService;
import com.campussync.backend.Service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/events", "/api/v1/events", "/api/events"})
public class EventCreatorController {

    private final EventParticipantService eventParticipantService;
    private final FeedbackService feedbackService;
    private final DynamicRegistrationFieldService dynamicFieldService;

    // ─── Participants ─────────────────────────────────────────────────────────

    @GetMapping("/{eventId}/participants")
    @PreAuthorize("isAuthenticated()")
    public PaginatedResponse<EventParticipantResponse> getParticipants(@PathVariable Long eventId,
                                                                       @RequestParam(required = false) Boolean paid,
                                                                       @RequestParam(required = false) Boolean attended,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return eventParticipantService.getParticipants(eventId, paid, attended, page, size);
    }

    /**
     * Feature 3: Export CSV — use ?attended=true to get attended-only participants.
     */
    @GetMapping("/{eventId}/participants/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> exportParticipants(@PathVariable Long eventId,
                                                                @RequestParam(required = false) Boolean paid,
                                                                @RequestParam(required = false) Boolean attended) {
        return eventParticipantService.exportParticipants(eventId, paid, attended);
    }

    @PostMapping({"/check-in", "/{eventId}/participants/check-in"})
    @PreAuthorize("isAuthenticated()")
    public EventCheckInResponse checkIn(@PathVariable(required = false) Long eventId,
                                        @Valid @RequestBody EventCheckInRequest request) {
        return eventParticipantService.checkIn(eventId, request);
    }

    @PostMapping("/{eventId}/broadcast")
    @PreAuthorize("isAuthenticated()")
    public EventBroadcastResponse broadcast(@PathVariable Long eventId,
                                            @Valid @RequestBody EventBroadcastRequest request) {
        return eventParticipantService.broadcast(eventId, request);
    }

    // ─── Certificate (Feature 4) ──────────────────────────────────────────────

    /**
     * GET /api/events/{eventId}/certificate/me
     * Download own certificate. Requires: certificateEnabled, attended.
     */
    @GetMapping("/{eventId}/certificate/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> getMyCertificate(@PathVariable Long eventId) {
        return eventParticipantService.generateCertificateForMe(eventId);
    }

    /**
     * GET /api/events/{eventId}/certificate/me/status
     * Check eligibility metadata before downloading.
     */
    @GetMapping("/{eventId}/certificate/me/status")
    @PreAuthorize("isAuthenticated()")
    public com.campussync.backend.Dto.CertificateEligibilityResponse getMyCertificateStatus(@PathVariable Long eventId) {
        return eventParticipantService.getMyCertificateStatus(eventId);
    }

    /**
     * GET /api/events/{eventId}/certificate/{userId}
     * Organizer/admin downloads certificate on behalf of a user.
     */
    @GetMapping("/{eventId}/certificate/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> generateCertificate(@PathVariable Long eventId,
                                                                 @PathVariable Long userId) {
        return eventParticipantService.generateCertificate(eventId, userId);
    }

    // ─── Dynamic Registration Fields (Feature 5) ─────────────────────────────

    /**
     * GET /api/events/{eventId}/registration-fields
     * Public — returns the registration fields for an event.
     */
    @GetMapping("/{eventId}/registration-fields")
    public List<EventRegistrationFieldResponse> getRegistrationFields(@PathVariable Long eventId) {
        return dynamicFieldService.getFields(eventId);
    }

    /**
     * PUT /api/events/{eventId}/registration-fields
     * Replace all registration fields for an event (creator/admin only).
     */
    @PutMapping("/{eventId}/registration-fields")
    @PreAuthorize("isAuthenticated()")
    public List<EventRegistrationFieldResponse> setRegistrationFields(@PathVariable Long eventId,
                                                                       @Valid @RequestBody List<EventRegistrationFieldRequest> fields) {
        return dynamicFieldService.setFields(eventId, fields);
    }

    // ─── Feedback ─────────────────────────────────────────────────────────────

    @PostMapping("/{eventId}/feedback")
    @PreAuthorize("isAuthenticated()")
    public FeedbackResponse submitFeedback(@PathVariable Long eventId,
                                           @Valid @RequestBody FeedbackRequest request) {
        return feedbackService.submitFeedback(eventId, request);
    }

    @GetMapping("/{eventId}/feedback")
    @PreAuthorize("isAuthenticated()")
    public PaginatedResponse<FeedbackResponse> getFeedback(@PathVariable Long eventId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return feedbackService.getFeedback(eventId, page, size);
    }
}

