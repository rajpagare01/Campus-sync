package com.campussync.backend.Controller;

import com.campussync.backend.Dto.EventAnalyticsResponse;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import com.campussync.backend.Service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping({"/events", "/api/v1/events"})
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // 🏫 Only SOCIETY / DEPARTMENT / ADMIN can create
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public com.campussync.backend.Dto.EventResponse createEvent(@Valid @RequestBody Event event) {
        return eventService.createEvent(event);
    }

    // 🌍 Public (or authenticated users) - Paginated version
    @GetMapping
    public PaginatedResponse<com.campussync.backend.Dto.EventResponse> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50; // Max page size

        return eventService.getAllEvents(page, size);
    }

    // 🌍 Public - Keep original endpoint for backward compatibility
    @GetMapping("/all")
    public List<com.campussync.backend.Dto.EventResponse> getAllEventsLegacy() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public com.campussync.backend.Dto.EventResponse getEventById(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public com.campussync.backend.Dto.EventResponse updateEvent(@PathVariable Long id, @RequestBody Event event) {
        return eventService.updateEvent(id, event);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public String deleteEvent(@PathVariable Long id) {
        return eventService.deleteEvent(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/status")
    public com.campussync.backend.Dto.EventResponse updateEventStatus(@PathVariable Long id, @RequestParam EventStatus status) {
        return eventService.updateEventStatus(id, status);
    }

    // 🌍 Public - Paginated search
    @GetMapping("/search")
    public PaginatedResponse<com.campussync.backend.Dto.EventResponse> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50; // Max page size

        return eventService.searchEvents(keyword, type, status, page, size);
    }

    // 🌍 Public - Keep original search for backward compatibility
    @GetMapping("/search/legacy")
    public List<com.campussync.backend.Dto.EventResponse> searchEventsLegacy(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status) {
        return eventService.searchEvents(keyword, type, status);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/analytics")
    public EventAnalyticsResponse getEventAnalytics(@PathVariable Long id) {
        return eventService.getEventAnalytics(id);
    }

    // 🆕 Get events created by user (paginated)
    @GetMapping("/creator/{creatorId}")
    public PaginatedResponse<com.campussync.backend.Dto.EventResponse> getEventsByCreator(
            @PathVariable Long creatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50; // Max page size

        return eventService.getEventsByCreator(creatorId, page, size);
    }
}
