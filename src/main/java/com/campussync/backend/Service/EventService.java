package com.campussync.backend.Service;

import com.campussync.backend.Dto.EventAnalyticsResponse;
import com.campussync.backend.Dto.EventRegistrationFieldResponse;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final RealtimeService realtimeService;
    private final SearchIndexService searchIndexService;
    private final DynamicRegistrationFieldService dynamicFieldService;
    private final com.campussync.backend.Repository.EventRegistrationFieldRepository fieldRepository;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        RegistrationRepository registrationRepository,
                        NotificationService notificationService,
                        RealtimeService realtimeService,
                        SearchIndexService searchIndexService,
                        com.campussync.backend.Repository.EventRegistrationFieldRepository fieldRepository,
                        @org.springframework.context.annotation.Lazy DynamicRegistrationFieldService dynamicFieldService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
        this.realtimeService = realtimeService;
        this.searchIndexService = searchIndexService;
        this.fieldRepository = fieldRepository;
        this.dynamicFieldService = dynamicFieldService;
    }

    @org.springframework.cache.annotation.CacheEvict(value = "eventsCache", allEntries = true)
    public com.campussync.backend.Dto.EventResponse createEvent(Event event) {
        User creator = getCurrentUser();

        event.setCreatedBy(creator);
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.DRAFT);
        }
        Event savedEvent = eventRepository.save(event);
        realtimeService.broadcastEventUpdate(savedEvent, "CREATED", "Event created");
        realtimeService.broadcastFeedRefresh("EVENT", savedEvent.getId(), "CREATED");
        searchIndexService.indexEvent(savedEvent);
        return mapToResponse(savedEvent);
    }

    @org.springframework.cache.annotation.Cacheable(value = "eventsCache")
    @Transactional(readOnly = true)
    public com.campussync.backend.Dto.PaginatedEventResponse getAllEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventPage = eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED, pageable);

        List<com.campussync.backend.Dto.EventResponse> content = mapEventsToResponse(eventPage.getContent());

        return new com.campussync.backend.Dto.PaginatedEventResponse(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages(),
                eventPage.isFirst(),
                eventPage.isLast(),
                eventPage.isEmpty()
        );
    }

    @org.springframework.cache.annotation.Cacheable(value = "eventsCache")
    @Transactional(readOnly = true)
    public List<com.campussync.backend.Dto.EventResponse> getAllEvents() {
        return mapEventsToResponse(eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED));
    }

    @Transactional // Bug #7 Fix: Ensure view count increment is atomic
    public com.campussync.backend.Dto.EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.setViewsCount(event.getViewsCount() + 1);
        return mapToResponse(eventRepository.save(event));
    }

    @org.springframework.cache.annotation.CacheEvict(value = "eventsCache", allEntries = true)
    public com.campussync.backend.Dto.EventResponse updateEvent(Long eventId, Event updatedEvent) {
        User currentUser = getCurrentUser();
        Event existing = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        boolean isOwner = existing.getCreatedBy() != null
                && existing.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized action");
        }

        existing.setTitle(updatedEvent.getTitle());
        existing.setDescription(updatedEvent.getDescription());
        existing.setVenue(updatedEvent.getVenue());
        existing.setDate(updatedEvent.getDate());
        existing.setType(updatedEvent.getType());
        existing.setPaid(updatedEvent.getPaid());
        existing.setPrice(updatedEvent.getPrice());
        existing.setImageUrl(updatedEvent.getImageUrl());
        existing.setCategory(updatedEvent.getCategory());
        existing.setMaxAttendees(updatedEvent.getMaxAttendees());
        if (updatedEvent.getStatus() != null) {
            existing.setStatus(updatedEvent.getStatus());
        }
        // Feature 4: propagate certificateEnabled flag
        if (updatedEvent.getCertificateEnabled() != null) {
            existing.setCertificateEnabled(updatedEvent.getCertificateEnabled());
        }

        Event savedEvent = eventRepository.save(existing);
        notificationService.notifyEventUpdate(savedEvent, "Details updated");
        realtimeService.broadcastEventUpdate(savedEvent, "UPDATED", "Event details updated");
        realtimeService.broadcastFeedRefresh("EVENT", savedEvent.getId(), "UPDATED");
        searchIndexService.indexEvent(savedEvent);
        return mapToResponse(savedEvent);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "eventsCache", allEntries = true)
    public String deleteEvent(Long eventId) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        boolean isOwner = event.getCreatedBy() != null
                && event.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized action");
        }

        eventRepository.delete(event);
        realtimeService.broadcastEventUpdate(event, "DELETED", "Event deleted");
        realtimeService.broadcastFeedRefresh("EVENT", eventId, "DELETED");
        searchIndexService.deleteEvent(eventId);
        return "Event deleted successfully";
    }

    @org.springframework.cache.annotation.CacheEvict(value = "eventsCache", allEntries = true)
    public com.campussync.backend.Dto.EventResponse updateEventStatus(Long eventId, EventStatus status) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        boolean isOwner = event.getCreatedBy() != null
                && event.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized action");
        }

        event.setStatus(status);
        Event savedEvent = eventRepository.save(event);
        notificationService.notifyEventUpdate(savedEvent, "Status changed to " + status);
        realtimeService.broadcastEventUpdate(savedEvent, "STATUS_CHANGED", "Status changed to " + status);
        realtimeService.broadcastFeedRefresh("EVENT", savedEvent.getId(), "UPDATED");
        searchIndexService.indexEvent(savedEvent);
        return mapToResponse(savedEvent);
    }

    @org.springframework.cache.annotation.Cacheable(value = "eventsCache")
    @Transactional(readOnly = true)
    public com.campussync.backend.Dto.PaginatedEventResponse searchEvents(String keyword, EventType type, EventStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Event> eventPage = eventRepository.searchEvents(normalizedKeyword, type, status, pageable);

        List<com.campussync.backend.Dto.EventResponse> content = mapEventsToResponse(eventPage.getContent());

        return new com.campussync.backend.Dto.PaginatedEventResponse(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages(),
                eventPage.isFirst(),
                eventPage.isLast(),
                eventPage.isEmpty()
        );
    }

    @org.springframework.cache.annotation.Cacheable(value = "eventsCache")
    @Transactional(readOnly = true)
    public List<com.campussync.backend.Dto.EventResponse> searchEvents(String keyword, EventType type, EventStatus status) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return mapEventsToResponse(eventRepository.searchEvents(normalizedKeyword, type, status));
    }

    public EventAnalyticsResponse getEventAnalytics(Long eventId) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        boolean isOwner = event.getCreatedBy() != null
                && event.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized action");
        }

        long totalRegistrations = registrationRepository.countByEventId(eventId);
        long activeRegistrations = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
        long cancelledRegistrations = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CANCELLED);
        long views = event.getViewsCount() == null ? 0L : event.getViewsCount();

        double conversionRate = views == 0 ? 0.0 : (activeRegistrations * 100.0) / views;
        double engagementScore = views + (activeRegistrations * 2.0) - cancelledRegistrations;

        EventAnalyticsResponse response = new EventAnalyticsResponse();
        response.setEventId(event.getId());
        response.setTitle(event.getTitle());
        response.setStatus(event.getStatus());
        response.setViews(views);
        response.setTotalRegistrations(totalRegistrations);
        response.setActiveRegistrations(activeRegistrations);
        response.setCancelledRegistrations(cancelledRegistrations);
        response.setRegistrationConversionRate(conversionRate);
        response.setEngagementScore(engagementScore);
        return response;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<com.campussync.backend.Dto.EventResponse> getEventsByCreator(Long creatorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventPage = eventRepository.findByCreatedByIdOrderByDateDesc(creatorId, pageable);

        List<com.campussync.backend.Dto.EventResponse> content = mapEventsToResponse(eventPage.getContent());

        return new PaginatedResponse<>(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages(),
                eventPage.isFirst(),
                eventPage.isLast(),
                eventPage.isEmpty()
        );
    }

    public com.campussync.backend.Dto.EventResponse mapToResponse(Event event) {
        long registrations = registrationRepository.countByEventIdAndStatus(event.getId(), com.campussync.backend.Model.RegistrationStatus.REGISTERED);
        List<EventRegistrationFieldResponse> fields = Collections.emptyList();
        try {
            fields = dynamicFieldService.getFields(event.getId());
        } catch (Exception ignored) { }
        return mapToResponse(event, registrations, fields);
    }
    
    public com.campussync.backend.Dto.EventResponse mapToResponse(Event event, long registrations, List<EventRegistrationFieldResponse> fields) {
        com.campussync.backend.Dto.EventResponse response = new com.campussync.backend.Dto.EventResponse();
        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setDescription(event.getDescription());
        response.setVenue(event.getVenue());
        response.setDate(event.getDate());
        response.setType(event.getType());
        response.setCategory(event.getCategory());
        response.setMaxAttendees(event.getMaxAttendees());
        response.setPaid(event.getPaid());
        response.setPrice(event.getPrice());
        response.setImageUrl(event.getImageUrl());
        response.setStatus(event.getStatus());
        response.setViewsCount(event.getViewsCount());
        response.setCreatedAt(event.getCreatedAt());

        // Feature 4: certificate enabled flag
        response.setCertificateEnabled(Boolean.TRUE.equals(event.getCertificateEnabled()));

        // Populate registration count
        response.setRegistrationCount(registrations);
        response.setRegistrationsCount(registrations);
        response.setEventDate(event.getDate());

        // Feature 5: include dynamic registration fields
        response.setRegistrationFields(fields == null || fields.isEmpty() ? Collections.emptyList() : fields);

        if (event.getCreatedBy() != null) {
            User creator = event.getCreatedBy();
            response.setCreatorId(creator.getId());

            com.campussync.backend.Dto.AuthorDto authorDto = new com.campussync.backend.Dto.AuthorDto();
            authorDto.setId(creator.getId());
            String fullName = creator.getName() != null ? creator.getName().trim() : "";
            String[] parts = fullName.split("\\s+", 2);
            authorDto.setFirstName(parts.length > 0 ? parts[0] : "");
            authorDto.setLastName(parts.length > 1 ? parts[1] : "");
            authorDto.setFullName(fullName);
            authorDto.setRole(creator.getRole() != null ? creator.getRole().name() : null);
            authorDto.setProfilePicture(creator.getProfilePictureUrl());
            authorDto.setAvatarUrl(creator.getProfilePictureUrl());
            response.setAuthor(authorDto);
        }

        return response;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private List<com.campussync.backend.Dto.EventResponse> mapEventsToResponse(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();
        
        List<Long> eventIds = events.stream().map(Event::getId).collect(java.util.stream.Collectors.toList());
        
        java.util.Map<Long, Long> regMap = new java.util.HashMap<>();
        List<Object[]> regCounts = registrationRepository.countByEventIdInAndStatus(eventIds, com.campussync.backend.Model.RegistrationStatus.REGISTERED);
        for (Object[] row : regCounts) {
            regMap.put((Long) row[0], (Long) row[1]);
        }
        
        java.util.Map<Long, List<EventRegistrationFieldResponse>> fieldsMap = new java.util.HashMap<>();
        List<com.campussync.backend.Model.EventRegistrationField> allFields = fieldRepository.findByEventIdInOrderByDisplayOrderAsc(eventIds);
        for (com.campussync.backend.Model.EventRegistrationField field : allFields) {
            fieldsMap.computeIfAbsent(field.getEvent().getId(), k -> new java.util.ArrayList<>())
                     .add(dynamicFieldService.toFieldResponse(field));
        }

        return events.stream().map(e -> {
            long reg = regMap.getOrDefault(e.getId(), 0L);
            List<EventRegistrationFieldResponse> fields = fieldsMap.getOrDefault(e.getId(), Collections.emptyList());
            return mapToResponse(e, reg, fields);
        }).collect(java.util.stream.Collectors.toList());
    }
}
