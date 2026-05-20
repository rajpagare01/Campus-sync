package com.campussync.backend.Service;

import com.campussync.backend.Dto.EventBroadcastRequest;
import com.campussync.backend.Dto.EventBroadcastResponse;
import com.campussync.backend.Dto.EventCheckInRequest;
import com.campussync.backend.Dto.EventCheckInResponse;
import com.campussync.backend.Dto.EventParticipantResponse;
import com.campussync.backend.Dto.EventRegistrationAnswerResponse;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Exception.ConflictException;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Exception.NotFoundException;
import com.campussync.backend.Model.AttendanceStatus;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventParticipantService {

    private static final DateTimeFormatter CSV_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final NotificationService notificationService;
    private final CertificateService certificateService;
    private final DynamicRegistrationFieldService dynamicFieldService;

    @Transactional(readOnly = true)
    public PaginatedResponse<EventParticipantResponse> getParticipants(Long eventId, Boolean paid, Boolean attended, int page, int size) {
        Event event = getManagedEvent(eventId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Registration> registrationPage = registrationRepository.findParticipantsByEventId(event.getId(), paid, attended, pageable);

        // Bulk-fetch answers for all registrations on this page
        List<Long> regIds = registrationPage.getContent().stream()
                .map(Registration::getId).collect(Collectors.toList());
        Map<Long, List<EventRegistrationAnswerResponse>> answersByReg =
                dynamicFieldService.getAnswersForRegistrations(regIds);

        Page<EventParticipantResponse> mapped = registrationPage.map(r ->
                toParticipantResponse(r, answersByReg.getOrDefault(r.getId(), Collections.emptyList())));

        return new PaginatedResponse<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages(),
                mapped.isFirst(),
                mapped.isLast(),
                mapped.isEmpty()
        );
    }

    /**
     * Feature 3: Enhanced CSV export with all required columns + dynamic field answers.
     * Filters to attended-only when ?attendance=attended.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ByteArrayResource> exportParticipants(Long eventId, Boolean paid, Boolean attended) {
        Event event = getManagedEvent(eventId);
        List<Registration> registrations = registrationRepository.findParticipantsByEventId(
                event.getId(), paid, attended, Pageable.unpaged()
        ).getContent();

        // Bulk fetch answers
        List<Long> regIds = registrations.stream().map(Registration::getId).collect(Collectors.toList());
        Map<Long, List<EventRegistrationAnswerResponse>> answersByReg =
                dynamicFieldService.getAnswersForRegistrations(regIds);

        // Collect dynamic field keys/labels for CSV header
        var fields = dynamicFieldService.getFields(eventId);

        StringBuilder csv = new StringBuilder();
        // Standard columns
        csv.append("fullName,email,role,department,year,registrationStatus,attendanceStatus,paymentStatus,checkedInAt,registeredAt");
        // Dynamic columns
        for (var field : fields) {
            csv.append(',').append(escapeCsv(field.getLabel()));
        }
        csv.append('\n');

        for (Registration reg : registrations) {
            User user = reg.getUser();
            List<EventRegistrationAnswerResponse> answers =
                    answersByReg.getOrDefault(reg.getId(), Collections.emptyList());

            csv.append(escapeCsv(user.getName())).append(',')
                    .append(escapeCsv(user.getEmail())).append(',')
                    .append(escapeCsv(user.getRole() != null ? user.getRole().name() : "")).append(',')
                    .append(escapeCsv(user.getDepartment())).append(',')
                    .append(escapeCsv(user.getYear())).append(',')
                    .append(reg.getStatus() != null ? reg.getStatus().name() : "").append(',')
                    .append(reg.getAttendanceStatus() != null ? reg.getAttendanceStatus().name() : "").append(',')
                    .append(reg.getPaymentStatus() != null ? reg.getPaymentStatus().name() : "").append(',')
                    .append(formatDate(reg.getCheckedInAt())).append(',')
                    .append(formatDate(reg.getCreatedAt()));

            // Dynamic field answers (order must match field order)
            Map<String, String> answerByKey = answers.stream()
                    .collect(Collectors.toMap(
                            EventRegistrationAnswerResponse::getFieldKey,
                            a -> a.getAnswerValue() == null ? "" : a.getAnswerValue(),
                            (a, b) -> a));
            for (var field : fields) {
                csv.append(',').append(escapeCsv(answerByKey.getOrDefault(field.getFieldKey(), "")));
            }
            csv.append('\n');
        }

        ByteArrayResource resource = new ByteArrayResource(csv.toString().getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("event-" + eventId + "-participants.csv")
                        .build().toString())
                .body(resource);
    }

    @Transactional
    public EventCheckInResponse checkIn(Long eventId, EventCheckInRequest request) {
        User currentUser = getCurrentUser();
        Registration registration;

        if (request.getQrCode() != null && !request.getQrCode().isBlank()) {
            QrCodeService.QrPayload payload = qrCodeService.parse(request.getQrCode());
            registration = registrationRepository.findById(payload.registrationId())
                    .orElseThrow(() -> new NotFoundException("Registration not found"));

            if (!registration.getUser().getId().equals(payload.userId())) {
                throw new ForbiddenOperationException("QR code does not match registration");
            }
        } else if (request.getRegistrationId() != null) {
            registration = registrationRepository.findById(request.getRegistrationId())
                    .orElseThrow(() -> new NotFoundException("Registration not found"));
        } else if (request.getUserId() != null && eventId != null) {
            registration = registrationRepository.findByUserIdAndEventId(request.getUserId(), eventId)
                    .orElseThrow(() -> new NotFoundException("Registration not found for this user and event"));
        } else {
            throw new IllegalArgumentException("Either qrCode, registrationId, or (userId and eventId) must be provided");
        }

        if (registration.getStatus() != RegistrationStatus.REGISTERED) {
            throw new ForbiddenOperationException("Only registered participants can be checked in");
        }

        Event event = registration.getEvent();

        if (eventId != null && !event.getId().equals(eventId)) {
            throw new ConflictException("Registration does not belong to the specified event");
        }

        assertCanManageEvent(currentUser, event);

        if (Boolean.TRUE.equals(registration.getAttended())) {
            throw new ConflictException("Participant already checked in");
        }

        registration.markCheckedIn(LocalDateTime.now());
        registrationRepository.save(registration);

        EventCheckInResponse response = new EventCheckInResponse();
        response.setRegistrationId(registration.getId());
        response.setEventId(event.getId());
        response.setEventTitle(event.getTitle());
        response.setUserId(registration.getUser().getId());
        response.setUserName(registration.getUser().getName());
        response.setAttended(true);
        response.setCheckedInAt(registration.getCheckedInAt());
        response.setMessage("Check-in successful");
        return response;
    }

    @Transactional
    public EventBroadcastResponse broadcast(Long eventId, EventBroadcastRequest request) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        assertCanManageEvent(currentUser, event);

        List<Registration> participants = registrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED).stream()
                .filter(registration -> registration.getUser() != null && registration.getUser().getId() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                registration -> registration.getUser().getId(),
                                Function.identity(),
                                (first, ignored) -> first
                        ),
                        registrationsByUserId -> List.copyOf(registrationsByUserId.values())
                ));
        int recipients = 0;

        for (Registration registration : participants) {
            notificationService.notifyEventBroadcast(event, registration.getUser(), currentUser, request.getMessage());
            recipients++;
        }

        EventBroadcastResponse response = new EventBroadcastResponse();
        response.setEventId(event.getId());
        response.setEventTitle(event.getTitle());
        response.setRecipients(recipients);
        response.setMessage(request.getMessage());
        response.setSentAt(LocalDateTime.now());
        return response;
    }

    /**
     * Feature 4: Generate certificate for a specific user (organizer/admin action).
     * Also handles self-download via generateCertificateForMe.
     */
    @Transactional
    public ResponseEntity<ByteArrayResource> generateCertificate(Long eventId, Long userId) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        boolean isSelfRequest = currentUser.getId().equals(userId);
        if (!isSelfRequest) {
            assertCanManageEvent(currentUser, event);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return buildCertificateResponse(event, user);
    }

    /**
     * Feature 4: Self-download — GET /api/events/{eventId}/certificate/me
     * Checks certificateEnabled, attendance, and marks download timestamp.
     */
    @Transactional
    public ResponseEntity<ByteArrayResource> generateCertificateForMe(Long eventId) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        return buildCertificateResponse(event, currentUser);
    }

    @Transactional(readOnly = true)
    public com.campussync.backend.Dto.CertificateEligibilityResponse getMyCertificateStatus(Long eventId) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        com.campussync.backend.Dto.CertificateEligibilityResponse response = new com.campussync.backend.Dto.CertificateEligibilityResponse();
        response.setEventId(eventId);
        response.setUserId(currentUser.getId());
        response.setCertificateEnabled(Boolean.TRUE.equals(event.getCertificateEnabled()));

        registrationRepository.findByUserIdAndEventId(currentUser.getId(), eventId).ifPresentOrElse(
                reg -> {
                    response.setRegistered(true);
                    response.setAttended(Boolean.TRUE.equals(reg.getAttended()));
                    response.setCertificateDownloadedAt(reg.getCertificateDownloadedAt());
                    response.setEligible(response.isCertificateEnabled() && response.isAttended());
                    response.setCertificateAvailable(response.isEligible());
                    if (!response.isCertificateEnabled()) {
                        response.setReason("Certificates are not enabled for this event.");
                    } else if (!response.isAttended()) {
                        response.setReason("You must attend the event to receive a certificate.");
                    }
                },
                () -> {
                    response.setRegistered(false);
                    response.setAttended(false);
                    response.setEligible(false);
                    response.setCertificateAvailable(false);
                    response.setReason("You are not registered for this event.");
                }
        );

        return response;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private ResponseEntity<ByteArrayResource> buildCertificateResponse(Event event, User user) {
        // Feature 4 eligibility checks
        if (!Boolean.TRUE.equals(event.getCertificateEnabled())) {
            throw new ForbiddenOperationException("Certificates are not enabled for this event");
        }

        Registration registration = registrationRepository.findByUserIdAndEventId(user.getId(), event.getId())
                .orElseThrow(() -> new ForbiddenOperationException("You are not registered for this event"));

        if (!Boolean.TRUE.equals(registration.getAttended())) {
            throw new ForbiddenOperationException("Certificate is available only for attended participants");
        }

        // Mark download timestamp on first download
        if (registration.getCertificateDownloadedAt() == null) {
            registration.setCertificateDownloadedAt(LocalDateTime.now());
            registrationRepository.save(registration);
        }

        CompletableFuture<byte[]> pdfFuture = certificateService.generateCertificate(event, user);
        byte[] pdfBytes = pdfFuture.join();
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("certificate-" + event.getId() + "-" + user.getId() + ".pdf")
                        .build().toString())
                .body(resource);
    }

    private EventParticipantResponse toParticipantResponse(Registration registration,
                                                            List<EventRegistrationAnswerResponse> answers) {
        EventParticipantResponse response = new EventParticipantResponse();
        response.setRegistrationId(registration.getId());
        response.setUserId(registration.getUser().getId());
        response.setUserName(registration.getUser().getName());
        response.setUserEmail(registration.getUser().getEmail());
        response.setRole(registration.getUser().getRole() != null ? registration.getUser().getRole().name() : null);
        response.setDepartment(registration.getUser().getDepartment());
        response.setYear(registration.getUser().getYear());
        response.setStatus(registration.getStatus().name());
        response.setPaymentRequired(Boolean.TRUE.equals(registration.getPaymentRequired()));
        response.setPaymentStatus(registration.getPaymentStatus().name());
        response.setAttended(Boolean.TRUE.equals(registration.getAttended()));
        response.setAttendanceStatus(registration.getAttendanceStatus() != null
                ? registration.getAttendanceStatus().name() : AttendanceStatus.NOT_CHECKED_IN.name());
        response.setCheckedInAt(registration.getCheckedInAt());
        response.setRegisteredAt(registration.getCreatedAt());
        response.setAnswers(answers);
        return response;
    }

    private Event getManagedEvent(Long eventId) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        assertCanManageEvent(currentUser, event);
        return event;
    }

    private void assertCanManageEvent(User currentUser, Event event) {
        boolean isOwner = event.getCreatedBy() != null && event.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("Unauthorized action");
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : value.format(CSV_DATE_TIME);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

