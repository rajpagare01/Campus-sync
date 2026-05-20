package com.campussync.backend.Service;

import com.campussync.backend.Dto.RegistrationResponse;
import com.campussync.backend.Dto.UserRegistrationResponse;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationPaymentStatus;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private RegistrationService registrationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerSendsEventRegistrationConfirmationEmail() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null)
        );

        User user = new User();
        user.setId(1L);
        user.setName("Student User");
        user.setEmail("student@example.com");
        user.setRole(Role.STUDENT);

        Event event = new Event();
        event.setId(10L);
        event.setTitle("Campus Hackathon");
        event.setVenue("Auditorium");
        event.setDate(LocalDateTime.of(2026, 4, 25, 10, 0));
        event.setPaid(false);

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.empty());
        when(qrCodeService.generateRegistrationQrCode(99L, 1L)).thenReturn("qr-token");
        when(registrationRepository.save(argThat(registrationFor(user, event))))
                .thenAnswer(invocation -> {
                    Registration registration = invocation.getArgument(0);
                    registration.setId(99L);
                    return registration;
                });

        RegistrationResponse response = registrationService.register(10L);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getUserName()).isEqualTo("Student User");
        assertThat(response.getEventTitle()).isEqualTo("Campus Hackathon");
        assertThat(response.getStatus()).isEqualTo("REGISTERED");
        assertThat(response.getQrCode()).isEqualTo("qr-token");

        verify(emailService).sendEventRegistrationConfirmation(
                eq(user),
                eq(event),
                argThat(registration ->
                        registration.getId().equals(99L)
                                && registration.getUser().equals(user)
                                && registration.getEvent().equals(event)
                                && RegistrationStatus.REGISTERED.equals(registration.getStatus()))
        );
    }

    @Test
    void getUserRegistrationsIncludesEventIdForFrontendMatching() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null)
        );

        User user = new User();
        user.setId(1L);
        user.setName("Student User");
        user.setEmail("student@example.com");
        user.setRole(Role.STUDENT);

        Event event = new Event();
        event.setId(10L);
        event.setTitle("Campus Hackathon");
        event.setVenue("Auditorium");

        Registration registration = new Registration();
        registration.setId(99L);
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setPaymentRequired(false);
        registration.setPaymentStatus(RegistrationPaymentStatus.NOT_REQUIRED);

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(registrationRepository.findByUserId(1L)).thenReturn(List.of(registration));

        List<UserRegistrationResponse> response = registrationService.getUserRegistrations(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRegistrationId()).isEqualTo(99L);
        assertThat(response.get(0).getEventId()).isEqualTo(10L);
        assertThat(response.get(0).getEventTitle()).isEqualTo("Campus Hackathon");
        assertThat(response.get(0).getEventVenue()).isEqualTo("Auditorium");
        assertThat(response.get(0).getStatus()).isEqualTo("REGISTERED");
    }

    private ArgumentMatcher<Registration> registrationFor(User user, Event event) {
        return registration -> registration != null
                && registration.getUser().equals(user)
                && registration.getEvent().equals(event)
                && RegistrationStatus.REGISTERED.equals(registration.getStatus());
    }
}