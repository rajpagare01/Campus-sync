package com.campussync.backend.Service;

import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RealtimeService realtimeService;

    @Mock
    private SearchIndexService searchIndexService;

    @InjectMocks
    private EventService eventService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateEventNotifiesRegisteredUsers() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setRole(Role.SOCIETY);

        Event existing = new Event();
        existing.setId(10L);
        existing.setTitle("Old title");
        existing.setCreatedBy(owner);
        existing.setStatus(EventStatus.PUBLISHED);

        Event update = new Event();
        update.setTitle("New title");
        update.setDescription("Updated details");
        update.setVenue("Auditorium");
        update.setDate(LocalDateTime.of(2026, 4, 25, 10, 0));

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = eventService.updateEvent(10L, update);

        assertThat(response.getTitle()).isEqualTo("New title");
        verify(notificationService).notifyEventUpdate(any(Event.class), eq("Details updated"));
    }

    @Test
    void updateEventStatusNotifiesRegisteredUsers() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setRole(Role.DEPARTMENT);

        Event event = new Event();
        event.setId(10L);
        event.setTitle("Campus event");
        event.setCreatedBy(owner);
        event.setStatus(EventStatus.DRAFT);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = eventService.updateEventStatus(10L, EventStatus.PUBLISHED);

        assertThat(response.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(notificationService).notifyEventUpdate(any(Event.class), eq("Status changed to PUBLISHED"));
    }
}
