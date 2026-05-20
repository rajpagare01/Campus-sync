package com.campussync.backend.Controller;

import com.campussync.backend.Dto.EventParticipantResponse;
import com.campussync.backend.Dto.EventRegistrationRequest;
import com.campussync.backend.Dto.EventRegistrationStatusResponse;
import com.campussync.backend.Dto.RegistrationResponse;
import com.campussync.backend.Dto.UserRegistrationResponse;
import com.campussync.backend.Service.RegistrationService;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping({"/registrations", "/api/v1/registrations", "/api/registrations"})
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Register for an event.
     * Feature 5: Optionally accepts dynamic registration field answers in request body.
     * Backward compatible: request body is optional.
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public RegistrationResponse register(@RequestParam Long eventId,
                                         @RequestBody(required = false) EventRegistrationRequest request) {
        return registrationService.register(eventId, request);
    }

    @GetMapping({"/event/{eventId}/my-status", "/event/{eventId}/status"})
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public EventRegistrationStatusResponse getMyRegistrationStatus(@PathVariable Long eventId) {
        return registrationService.getMyRegistrationStatus(eventId);
    }

    @GetMapping("/status/{eventId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public EventRegistrationStatusResponse getMyRegistrationStatusAlias(@PathVariable Long eventId) {
        return registrationService.getMyRegistrationStatus(eventId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping({"/me", "/user/{userId}"})
    public List<UserRegistrationResponse> getUserRegistrations(@PathVariable(required = false) Long userId) {
        return registrationService.getUserRegistrations(userId);
    }

    @PreAuthorize("hasAnyRole('SOCIETY','DEPARTMENT','ADMIN')")
    @GetMapping("/event/{eventId}")
    public List<EventParticipantResponse> getEventParticipants(@PathVariable Long eventId) {
        return registrationService.getEventParticipants(eventId);
    }

    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    @PutMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id) {
        return registrationService.cancelRegistration(id);
    }
}

