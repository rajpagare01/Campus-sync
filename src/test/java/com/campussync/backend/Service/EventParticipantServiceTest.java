package com.campussync.backend.Service;

import com.campussync.backend.Dto.EventCheckInRequest;
import com.campussync.backend.Dto.EventCheckInResponse;
import com.campussync.backend.Dto.EventBroadcastRequest;
import com.campussync.backend.Dto.EventBroadcastResponse;
import com.campussync.backend.Exception.ConflictException;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Model.AttendanceStatus;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationStatus;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventParticipantServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CertificateService certificateService;

    @InjectMocks
    private EventParticipantService eventParticipantService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkInMarksAttendanceForRegisteredParticipant() {
        User owner = ownerUser();
        User participant = participantUser();
        Event event = managedEvent(owner);

        Registration registration = new Registration();
        registration.setId(44L);
        registration.setUser(participant);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setAttended(false);
        registration.setAttendanceStatus(AttendanceStatus.NOT_CHECKED_IN);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(qrCodeService.parse("qr-token")).thenReturn(new QrCodeService.QrPayload(44L, 2L));
        when(registrationRepository.findById(44L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventCheckInRequest request = new EventCheckInRequest();
        request.setQrCode("qr-token");

        EventCheckInResponse response = eventParticipantService.checkIn(12L, request);

        assertThat(response.getRegistrationId()).isEqualTo(44L);
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.isAttended()).isTrue();
        assertThat(registration.getAttended()).isTrue();
        assertThat(registration.getCheckedInAt()).isNotNull();
        verify(registrationRepository).save(registration);
    }

    @Test
    void checkInRejectsDuplicateAttendance() {
        User owner = ownerUser();
        User participant = participantUser();
        Event event = managedEvent(owner);

        Registration registration = new Registration();
        registration.setId(44L);
        registration.setUser(participant);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setAttended(true);
        registration.setAttendanceStatus(AttendanceStatus.CHECKED_IN);
        registration.setCheckedInAt(LocalDateTime.now().minusMinutes(2));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(qrCodeService.parse("qr-token")).thenReturn(new QrCodeService.QrPayload(44L, 2L));
        when(registrationRepository.findById(44L)).thenReturn(Optional.of(registration));

        EventCheckInRequest request = new EventCheckInRequest();
        request.setQrCode("qr-token");

        assertThatThrownBy(() -> eventParticipantService.checkIn(12L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Participant already checked in");
    }

    @Test
    void generateCertificateRejectsParticipantWithoutAttendance() {
        User owner = ownerUser();
        User participant = participantUser();
        Event event = managedEvent(owner);

        Registration registration = new Registration();
        registration.setId(44L);
        registration.setUser(participant);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setAttended(false);
        registration.setAttendanceStatus(AttendanceStatus.NOT_CHECKED_IN);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(eventRepository.findById(12L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserIdAndEventId(2L, 12L)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> eventParticipantService.generateCertificate(12L, 2L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Certificate is available only for attended participants");

        verify(certificateService, never()).generateCertificate(any(), any());
    }

    @Test
    void broadcastTargetsRegisteredParticipantsUsingCurrentSender() {
        User owner = ownerUser();
        User participant = participantUser();
        Event event = managedEvent(owner);

        Registration registered = new Registration();
        registered.setId(44L);
        registered.setUser(participant);
        registered.setEvent(event);
        registered.setStatus(RegistrationStatus.REGISTERED);

        Registration duplicateForSameUser = new Registration();
        duplicateForSameUser.setId(45L);
        duplicateForSameUser.setUser(participant);
        duplicateForSameUser.setEvent(event);
        duplicateForSameUser.setStatus(RegistrationStatus.REGISTERED);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(eventRepository.findById(12L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndStatus(12L, RegistrationStatus.REGISTERED))
                .thenReturn(List.of(registered, duplicateForSameUser));

        EventBroadcastRequest request = new EventBroadcastRequest();
        request.setMessage("Report at Hall A");

        EventBroadcastResponse response = eventParticipantService.broadcast(12L, request);

        assertThat(response.getRecipients()).isEqualTo(1);
        verify(notificationService).notifyEventBroadcast(event, participant, owner, "Report at Hall A");
    }

    private User ownerUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("owner@example.com");
        user.setName("Event Owner");
        user.setRole(Role.SOCIETY);
        return user;
    }

    private User participantUser() {
        User user = new User();
        user.setId(2L);
        user.setEmail("student@example.com");
        user.setName("Student User");
        user.setRole(Role.STUDENT);
        return user;
    }

    private Event managedEvent(User owner) {
        Event event = new Event();
        event.setId(12L);
        event.setTitle("CampusSync Summit");
        event.setCreatedBy(owner);
        return event;
    }
}
