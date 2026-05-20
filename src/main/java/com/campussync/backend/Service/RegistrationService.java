package com.campussync.backend.Service;

import com.campussync.backend.Dto.EventParticipantResponse;
import com.campussync.backend.Dto.EventRegistrationRequest;
import com.campussync.backend.Dto.EventRegistrationStatusResponse;
import com.campussync.backend.Dto.PaymentCheckoutRequest;
import com.campussync.backend.Dto.PaymentCheckoutResponse;
import com.campussync.backend.Dto.RegistrationResponse;
import com.campussync.backend.Dto.UserRegistrationResponse;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Exception.NotFoundException;
import com.campussync.backend.Model.AttendanceStatus;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationPaymentStatus;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;
    private final QrCodeService qrCodeService;
    private final DynamicRegistrationFieldService dynamicRegistrationFieldService;

    @Transactional
    public RegistrationResponse register(Long eventId) {
        return register(eventId, null);
    }

    @Transactional
    public RegistrationResponse register(Long eventId, EventRegistrationRequest request) {
        User user = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        Registration existingRegistration = registrationRepository.findByUserIdAndEventId(user.getId(), eventId)
                .orElse(null);
        if (existingRegistration != null && !existingRegistration.isCancelled()) {
            return toAlreadyRegisteredResponse(existingRegistration);
        }

        if (Boolean.TRUE.equals(event.getPaid()) && event.getPrice() > 0) {
            return registerPaidEvent(eventId);
        }

        return registerFreeEvent(user, event, request);
    }

    private RegistrationResponse registerFreeEvent(User user, Event event) {
        return registerFreeEvent(user, event, null);
    }

    private RegistrationResponse registerFreeEvent(User user, Event event, EventRegistrationRequest request) {
        Registration registration = new Registration();
        registration.setUser(user);
        registration.setEvent(event);
        registration.markRegistered();
        registration.markPaymentNotRequired();
        registration.setAttendanceStatus(AttendanceStatus.NOT_CHECKED_IN);
        registration.setAttended(false);
        registration.setCreatedAt(LocalDateTime.now());
        registration = registrationRepository.save(registration);
        registration.setQrCode(qrCodeService.generateRegistrationQrCode(registration.getId(), user.getId()));
        registrationRepository.save(registration);

        // Feature 5: Save dynamic registration field answers
        if (request != null && request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            dynamicRegistrationFieldService.saveAnswers(registration, request.getAnswers());
        }

        emailService.sendEventRegistrationConfirmation(user, event, registration);

        RegistrationResponse response = new RegistrationResponse();
        response.setId(registration.getId());
        response.setUserName(user.getName());
        response.setEventTitle(event.getTitle());
        response.setEventId(event.getId());
        response.setStatus(registration.getStatus() != null ? registration.getStatus().name() : "REGISTERED");
        response.setPaymentRequired(false);
        response.setPaymentStatus(registration.getPaymentStatus() != null ? registration.getPaymentStatus().name() : "NOT_REQUIRED");
        response.setAlreadyRegistered(false);
        response.setCanCancel(true);
        response.setQrCode(registration.getQrCode());
        return response;
    }

    private RegistrationResponse registerPaidEvent(Long eventId) {
        PaymentCheckoutRequest checkoutRequest = new PaymentCheckoutRequest();
        checkoutRequest.setEventId(eventId);

        PaymentCheckoutResponse checkout = paymentService.createCheckoutSession(checkoutRequest);
        Registration registration = registrationRepository.findById(checkout.getRegistrationId())
                .orElseThrow(() -> new NotFoundException("Registration not found after checkout creation"));

        RegistrationResponse response = new RegistrationResponse();
        response.setId(registration.getId());
        response.setUserName(registration.getUser().getName());
        response.setEventTitle(registration.getEvent().getTitle());
        response.setEventId(registration.getEvent().getId());
        response.setStatus(registration.getStatus() != null ? registration.getStatus().name() : "PAYMENT_PENDING");
        response.setPaymentRequired(true);
        response.setPaymentStatus(registration.getPaymentStatus() != null ? registration.getPaymentStatus().name() : "PENDING");
        response.setPaymentOrderId(checkout.getOrderId());
        response.setProviderOrderId(checkout.getProviderOrderId());
        response.setRazorpayKeyId(checkout.getRazorpayKeyId());
        response.setAmountInMinorUnits(checkout.getAmountInMinorUnits());
        response.setCurrency(checkout.getCurrency());
        response.setAlreadyRegistered(false);
        response.setCanCancel(true);
        response.setQrCode(registration.getQrCode());
        return response;
    }

    public EventRegistrationStatusResponse getMyRegistrationStatus(Long eventId) {
        User user = getCurrentUser();
        Registration registration = registrationRepository.findByUserIdAndEventId(user.getId(), eventId)
                .orElse(null);

        EventRegistrationStatusResponse response = new EventRegistrationStatusResponse();
        response.setEventId(eventId);

        if (registration == null || registration.isCancelled()) {
            response.setRegistered(false);
            response.setCanCancel(false);
            response.setStatus(registration != null && registration.getStatus() != null ? registration.getStatus().name() : null);
            response.setPaymentStatus(registration != null && registration.getPaymentStatus() != null ? registration.getPaymentStatus().name() : null);
            response.setPaymentRequired(registration != null && Boolean.TRUE.equals(registration.getPaymentRequired()));
            return response;
        }

        response.setRegistered(registration.isRegistered()
                || registration.getPaymentStatus() == RegistrationPaymentStatus.PAID);
        response.setCanCancel(true);
        response.setRegistrationId(registration.getId());
        response.setStatus(registration.getStatus() != null ? registration.getStatus().name() : "REGISTERED");
        response.setPaymentStatus(registration.getPaymentStatus() != null ? registration.getPaymentStatus().name() : "NOT_REQUIRED");
        response.setPaymentRequired(Boolean.TRUE.equals(registration.getPaymentRequired()));
        return response;
    }

    public List<UserRegistrationResponse> getUserRegistrations(Long userId) {
        User currentUser = getCurrentUser();
        Long targetUserId = userId != null ? userId : currentUser.getId();

        // Security check: Only user themselves or ADMIN can view registrations
        if (!targetUserId.equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Unauthorized: You can only view your own registrations");
        }

        List<Registration> registrations = registrationRepository.findByUserId(targetUserId);

        return registrations.stream().map(reg -> {
            UserRegistrationResponse dto = new UserRegistrationResponse();
            dto.setRegistrationId(reg.getId());
            dto.setUserId(reg.getUser().getId());
            dto.setEventId(reg.getEvent().getId());
            dto.setEventTitle(reg.getEvent().getTitle());
            dto.setEventVenue(reg.getEvent().getVenue());
            dto.setStatus(reg.getStatus() != null ? reg.getStatus().name() : "REGISTERED");
            dto.setPaymentRequired(Boolean.TRUE.equals(reg.getPaymentRequired()));
            dto.setPaymentStatus(reg.getPaymentStatus() != null ? reg.getPaymentStatus().name() : "NOT_REQUIRED");
            dto.setAttended(Boolean.TRUE.equals(reg.getAttended()));
            dto.setCheckedInAt(reg.getCheckedInAt());
            dto.setQrCode(reg.getQrCode());
            return dto;
        }).toList();
    }

    public List<EventParticipantResponse> getEventParticipants(Long eventId) {
        List<Registration> registrations = registrationRepository.findByEventId(eventId);

        return registrations.stream().map(reg -> {
            EventParticipantResponse dto = new EventParticipantResponse();
            dto.setRegistrationId(reg.getId());
            dto.setUserName(reg.getUser().getName());
            dto.setStatus(reg.getStatus().name());
            return dto;
        }).toList();
    }

    public String cancelRegistration(Long id) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Registration not found"));

        User currentUser = getCurrentUser();
        String email = currentUser.getEmail();

        if (!registration.getUser().getEmail().equals(email) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Unauthorized action");
        }

        registration.markCancelled();
        if (Boolean.TRUE.equals(registration.getPaymentRequired())
                && registration.getPaymentStatus() == RegistrationPaymentStatus.PENDING) {
            registration.markPaymentCancelled();
        }
        registrationRepository.save(registration);

        return "Registration cancelled";
    }

    private RegistrationResponse toAlreadyRegisteredResponse(Registration registration) {
        RegistrationResponse response = new RegistrationResponse();
        response.setId(registration.getId());
        response.setUserName(registration.getUser().getName());
        response.setEventTitle(registration.getEvent().getTitle());
        response.setEventId(registration.getEvent().getId());
        response.setStatus(registration.getStatus() != null ? registration.getStatus().name() : "REGISTERED");
        response.setPaymentRequired(Boolean.TRUE.equals(registration.getPaymentRequired()));
        response.setPaymentStatus(registration.getPaymentStatus() != null ? registration.getPaymentStatus().name() : "NOT_REQUIRED");
        response.setPaymentOrderId(registration.getPaymentOrderId());
        response.setAlreadyRegistered(true);
        response.setCanCancel(true);
        response.setQrCode(registration.getQrCode());
        return response;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
