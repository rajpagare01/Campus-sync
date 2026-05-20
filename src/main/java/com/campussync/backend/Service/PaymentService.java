package com.campussync.backend.Service;

import com.campussync.backend.Dto.PaymentCheckoutRequest;
import com.campussync.backend.Dto.PaymentCheckoutResponse;
import com.campussync.backend.Dto.PaymentOrderResponse;
import com.campussync.backend.Dto.RefundRequest;
import com.campussync.backend.Exception.ConflictException;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Exception.NotFoundException;
import com.campussync.backend.Model.AttendanceStatus;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.PaymentOrder;
import com.campussync.backend.Model.PaymentOrderStatus;
import com.campussync.backend.Model.PaymentProvider;
import com.campussync.backend.Model.PaymentTransaction;
import com.campussync.backend.Model.PaymentTransactionStatus;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationPaymentStatus;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.PaymentOrderRepository;
import com.campussync.backend.Repository.PaymentTransactionRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentGateway paymentGateway;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EmailService emailService;
    private final RealtimeService realtimeService;
    private final QrCodeService qrCodeService;

    @Value("${payment.success-url}")
    private String defaultSuccessUrl;

    @Value("${payment.cancel-url}")
    private String defaultCancelUrl;

    @Value("${payment.currency:INR}")
    private String currency;

    @Transactional
    public PaymentCheckoutResponse createCheckoutSession(PaymentCheckoutRequest request) {
        User user = getCurrentUser();
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validatePaidEvent(event);

        Registration registration = prepareRegistrationForPayment(user, event);
        String clientRequestId = resolveIdempotencyKey(request, user, event);
        PaymentOrder order = createPendingOrder(user, event, registration, clientRequestId);

        try {
            GatewayCheckoutSession gatewayOrder = paymentGateway.createCheckoutSession(
                    order,
                    normalizeUrl(request.getSuccessUrl(), defaultSuccessUrl),
                    normalizeUrl(request.getCancelUrl(), defaultCancelUrl)
            );

            order.setProviderOrderId(gatewayOrder.providerOrderId());
            order.setCheckoutPublicKey(gatewayOrder.publicKey());
            paymentOrderRepository.save(order);

            registration.setPaymentOrderId(order.getId());
            registrationRepository.save(registration);

            recordTransaction(order, PaymentTransactionStatus.INITIATED, "ORDER_CREATED",
                    gatewayOrder.providerOrderId(), "razorpay.order.created", null, null);

            PaymentCheckoutResponse response = new PaymentCheckoutResponse();
            response.setOrderId(order.getId());
            response.setRegistrationId(registration.getId());
            response.setProviderOrderId(order.getProviderOrderId());
            response.setRazorpayKeyId(order.getCheckoutPublicKey());
            response.setAmountInMinorUnits(order.getAmountInMinorUnits());
            response.setCurrency(order.getCurrency());
            response.setPaymentStatus(registration.getPaymentStatus().name());
            return response;
        } catch (RuntimeException ex) {
            order.setStatus(PaymentOrderStatus.FAILED);
            order.setFailureReason(ex.getMessage());
            paymentOrderRepository.save(order);

            registration.markCancelled();
            registration.markPaymentFailed();
            registrationRepository.save(registration);

            recordTransaction(order, PaymentTransactionStatus.FAILED, "ORDER_CREATED",
                    null, "razorpay.order.failed", ex.getMessage(), null);
            throw ex;
        }
    }

    @Transactional
    public String handleWebhook(String payload, String signatureHeader) {
        GatewayWebhookResult result = paymentGateway.parseWebhook(payload, signatureHeader);
        if (result == null) {
            return "Ignored";
        }
        if (result.eventId() != null && paymentTransactionRepository.existsByGatewayEventId(result.eventId())) {
            return "Already processed";
        }

        PaymentOrder order = findOrderForWebhook(result);
        Registration registration = order.getRegistration();

        switch (result.type()) {
            case "order.paid", "payment.captured" -> {
                if (order.getStatus() == PaymentOrderStatus.PAID && registration.isRegistered()) {
                    return "Already processed";
                }

                order.setStatus(PaymentOrderStatus.PAID);
                order.setProviderPaymentId(result.providerPaymentId());
                order.setPaidAt(LocalDateTime.now());
                order.setFailureReason(null);
                paymentOrderRepository.save(order);

                registration.markRegistered();
                registration.markPaymentPaid();
                registration.setPaymentOrderId(order.getId());
                if (registration.getQrCode() == null || registration.getQrCode().isBlank()) {
                    registration.setQrCode(qrCodeService.generateRegistrationQrCode(
                            registration.getId(),
                            registration.getUser().getId()
                    ));
                }
                registrationRepository.save(registration);

                recordTransaction(order, PaymentTransactionStatus.SUCCEEDED, "WEBHOOK_CONFIRMATION",
                        result.providerPaymentId(), result.type(), null, result.eventId());
                emailService.sendPaymentReceipt(order.getUser(), order.getEvent(), order);
                emailService.sendEventRegistrationConfirmation(order.getUser(), order.getEvent(), registration);
                realtimeService.broadcastEventUpdate(order.getEvent(), "REGISTRATION_CONFIRMED",
                        "A paid registration was confirmed for " + order.getEvent().getTitle());
                return "Payment processed";
            }
            case "payment.failed" -> {
                order.setStatus(PaymentOrderStatus.FAILED);
                order.setProviderPaymentId(result.providerPaymentId());
                order.setFailureReason(result.failureMessage());
                paymentOrderRepository.save(order);

                registration.markCancelled();
                registration.markPaymentFailed();
                registrationRepository.save(registration);

                recordTransaction(order, PaymentTransactionStatus.FAILED, "WEBHOOK_CONFIRMATION",
                        result.providerPaymentId(), result.type(), result.failureMessage(), result.eventId());
                return "Payment failed";
            }
            default -> {
                return "Ignored";
            }
        }
    }

    @Transactional(readOnly = true)
    public PaymentOrderResponse getOrder(Long orderId) {
        return mapOrder(paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Payment order not found")));
    }

    @Transactional(readOnly = true)
    public List<PaymentOrderResponse> getMyOrders() {
        User currentUser = getCurrentUser();
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapOrder)
                .toList();
    }

    @Transactional
    public PaymentOrderResponse refundOrder(Long orderId, RefundRequest request) {
        User currentUser = getCurrentUser();
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Payment order not found"));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isEventOwner = order.getEvent().getCreatedBy() != null
                && order.getEvent().getCreatedBy().getId().equals(currentUser.getId());

        if (!isAdmin && !isEventOwner) {
            throw new ForbiddenOperationException("Unauthorized action");
        }
        if (order.getStatus() != PaymentOrderStatus.PAID) {
            throw new ConflictException("Only paid orders can be refunded");
        }

        String refundReference = paymentGateway.refundPayment(order, request.getReason());

        order.setStatus(PaymentOrderStatus.REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        paymentOrderRepository.save(order);

        Registration registration = order.getRegistration();
        registration.markCancelled();
        registration.markPaymentRefunded();
        registration.setAttendanceStatus(AttendanceStatus.NOT_CHECKED_IN);
        registration.setAttended(false);
        registration.setCheckedInAt(null);
        registrationRepository.save(registration);

        recordTransaction(order, PaymentTransactionStatus.REFUNDED, "REFUND",
                refundReference, "razorpay.refund.created", request.getReason(), null);

        realtimeService.broadcastEventUpdate(order.getEvent(), "REFUND",
                "A paid registration was refunded for " + order.getEvent().getTitle());
        return mapOrder(order);
    }

    private PaymentOrder findOrderForWebhook(GatewayWebhookResult result) {
        if (result.orderId() != null) {
            return paymentOrderRepository.findById(result.orderId())
                    .orElseThrow(() -> new NotFoundException("Payment order not found"));
        }
        if (result.providerOrderId() != null) {
            return paymentOrderRepository.findByProviderOrderId(result.providerOrderId())
                    .orElseThrow(() -> new NotFoundException("Payment order not found"));
        }
        throw new NotFoundException("Payment order not found");
    }

    private Registration prepareRegistrationForPayment(User user, Event event) {
        Registration registration = registrationRepository.findByUserIdAndEventId(user.getId(), event.getId())
                .orElseGet(Registration::new);

        if (registration.getId() != null && isCompletedPaidRegistration(registration)) {
            throw new ConflictException("Already registered for this event");
        }

        registration.setUser(user);
        registration.setEvent(event);
        registration.markPaymentPending();
        registration.setQrCode(null);
        registration.setAttended(false);
        registration.setAttendanceStatus(AttendanceStatus.NOT_CHECKED_IN);
        registration.setCheckedInAt(null);
        if (registration.getCreatedAt() == null) {
            registration.setCreatedAt(LocalDateTime.now());
        }

        return registrationRepository.save(registration);
    }

    private PaymentOrder createPendingOrder(User user, Event event, Registration registration, String clientRequestId) {
        PaymentOrder order = paymentOrderRepository.findByClientRequestIdAndUserId(clientRequestId, user.getId())
                .or(() -> paymentOrderRepository.findByRegistrationId(registration.getId()))
                .orElseGet(PaymentOrder::new);

        order.setUser(user);
        order.setEvent(event);
        order.setRegistration(registration);
        order.setProvider(PaymentProvider.RAZORPAY);
        order.setStatus(PaymentOrderStatus.PENDING);
        order.setCurrency(currency.toUpperCase(Locale.ROOT));
        order.setAmountInMinorUnits(toMinorUnits(event.getPrice()));
        order.setClientRequestId(clientRequestId);
        order.setProviderOrderId(null);
        order.setProviderPaymentId(null);
        order.setCheckoutPublicKey(null);
        order.setPaidAt(null);
        order.setRefundedAt(null);
        order.setFailureReason(null);
        return paymentOrderRepository.save(order);
    }

    private void recordTransaction(PaymentOrder order,
                                   PaymentTransactionStatus status,
                                   String transactionType,
                                   String providerReference,
                                   String gatewayEventType,
                                   String failureReason,
                                   String gatewayEventId) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setStatus(status);
        transaction.setTransactionType(transactionType);
        transaction.setGatewayEventId(gatewayEventId);
        transaction.setProviderReference(providerReference);
        transaction.setAmountInMinorUnits(order.getAmountInMinorUnits());
        transaction.setCurrency(order.getCurrency());
        transaction.setGatewayEventType(gatewayEventType);
        transaction.setFailureReason(failureReason);
        paymentTransactionRepository.save(transaction);
    }

    private PaymentOrderResponse mapOrder(PaymentOrder order) {
        PaymentOrderResponse response = new PaymentOrderResponse();
        response.setId(order.getId());
        response.setProvider(order.getProvider());
        response.setStatus(order.getStatus());
        response.setEventId(order.getEvent().getId());
        response.setEventTitle(order.getEvent().getTitle());
        response.setRegistrationId(order.getRegistration().getId());
        response.setAmountInMinorUnits(order.getAmountInMinorUnits());
        response.setCurrency(order.getCurrency());
        response.setProviderOrderId(order.getProviderOrderId());
        response.setProviderPaymentId(order.getProviderPaymentId());
        response.setCheckoutPublicKey(order.getCheckoutPublicKey());
        response.setFailureReason(order.getFailureReason());
        response.setPaidAt(order.getPaidAt());
        response.setRefundedAt(order.getRefundedAt());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    private void validatePaidEvent(Event event) {
        if (event.getPaid() == null || !event.getPaid()) {
            throw new ConflictException("This event does not require payment");
        }
        if (event.getPrice() <= 0) {
            throw new ConflictException("Paid event price must be greater than zero");
        }
    }

    private boolean isCompletedPaidRegistration(Registration registration) {
        return registration.getStatus() == RegistrationStatus.REGISTERED
                || registration.getPaymentStatus() == RegistrationPaymentStatus.PAID;
    }

    private String resolveIdempotencyKey(PaymentCheckoutRequest request, User user, Event event) {
        String provided = request.getIdempotencyKey();
        if (provided != null && !provided.isBlank()) {
            return provided.trim();
        }
        return ("checkout:%d:%d").formatted(user.getId(), event.getId()).toLowerCase(Locale.ROOT);
    }

    private String normalizeUrl(String provided, String fallback) {
        return provided == null || provided.isBlank() ? fallback : provided.trim();
    }

    private long toMinorUnits(double amount) {
        return BigDecimal.valueOf(amount)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ForbiddenOperationException("Unauthorized");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
