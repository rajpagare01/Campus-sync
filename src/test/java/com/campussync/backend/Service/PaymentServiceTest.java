package com.campussync.backend.Service;

import com.campussync.backend.Dto.PaymentCheckoutRequest;
import com.campussync.backend.Dto.RefundRequest;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.PaymentOrder;
import com.campussync.backend.Model.PaymentOrderStatus;
import com.campussync.backend.Model.PaymentProvider;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RealtimeService realtimeService;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "defaultSuccessUrl", "http://localhost:3000/success");
        ReflectionTestUtils.setField(paymentService, "defaultCancelUrl", "http://localhost:3000/cancel");
        ReflectionTestUtils.setField(paymentService, "currency", "INR");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCheckoutSessionCreatesRazorpayOrderAndPendingRegistration() {
        User user = studentUser();
        Event event = paidEvent();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null)
        );

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.empty());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration registration = invocation.getArgument(0);
            if (registration.getId() == null) {
                registration.setId(55L);
            }
            return registration;
        });
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> {
            PaymentOrder order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(99L);
            }
            return order;
        });
        when(paymentGateway.createCheckoutSession(any(PaymentOrder.class), any(String.class), any(String.class)))
                .thenReturn(new GatewayCheckoutSession("order_test_123", "rzp_test_public"));

        PaymentCheckoutRequest request = new PaymentCheckoutRequest();
        request.setEventId(10L);

        var response = paymentService.createCheckoutSession(request);

        assertThat(response.getOrderId()).isEqualTo(99L);
        assertThat(response.getRegistrationId()).isEqualTo(55L);
        assertThat(response.getProviderOrderId()).isEqualTo("order_test_123");
        assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_test_public");
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        verify(qrCodeService, never()).generateRegistrationQrCode(any(), any());
        verify(paymentTransactionRepository).save(any());
    }

    @Test
    void createCheckoutSessionReusesExistingPendingRegistrationAndOrderForRetry() {
        User user = studentUser();
        Event event = paidEvent();

        Registration registration = new Registration();
        registration.setId(55L);
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.PAYMENT_PENDING);
        registration.setPaymentRequired(true);
        registration.setPaymentStatus(RegistrationPaymentStatus.PENDING);
        registration.setPaymentOrderId(99L);

        PaymentOrder existingOrder = new PaymentOrder();
        existingOrder.setId(99L);
        existingOrder.setUser(user);
        existingOrder.setEvent(event);
        existingOrder.setRegistration(registration);
        existingOrder.setProvider(PaymentProvider.RAZORPAY);
        existingOrder.setStatus(PaymentOrderStatus.FAILED);
        existingOrder.setProviderOrderId("old-order");
        existingOrder.setProviderPaymentId("old-payment");
        existingOrder.setCheckoutPublicKey("old-key");
        existingOrder.setFailureReason("Checkout closed");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null)
        );

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentOrderRepository.findByRegistrationId(55L)).thenReturn(Optional.of(existingOrder));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.createCheckoutSession(any(PaymentOrder.class), any(String.class), any(String.class)))
                .thenReturn(new GatewayCheckoutSession("order_retry_456", "rzp_retry_public"));

        PaymentCheckoutRequest request = new PaymentCheckoutRequest();
        request.setEventId(10L);

        var response = paymentService.createCheckoutSession(request);

        assertThat(response.getOrderId()).isEqualTo(99L);
        assertThat(response.getRegistrationId()).isEqualTo(55L);
        assertThat(response.getProviderOrderId()).isEqualTo("order_retry_456");
        assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_retry_public");
        assertThat(existingOrder.getStatus()).isEqualTo(PaymentOrderStatus.PENDING);
        assertThat(existingOrder.getProviderPaymentId()).isNull();
        assertThat(existingOrder.getFailureReason()).isNull();
    }

    @Test
    void handleOrderPaidWebhookMarksOrderPaidAndRegistrationRegistered() {
        User user = studentUser();
        Event event = paidEvent();

        Registration registration = new Registration();
        registration.setId(55L);
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.PAYMENT_PENDING);
        registration.setPaymentRequired(true);
        registration.setPaymentStatus(RegistrationPaymentStatus.PENDING);

        PaymentOrder order = new PaymentOrder();
        order.setId(99L);
        order.setProvider(PaymentProvider.RAZORPAY);
        order.setStatus(PaymentOrderStatus.PENDING);
        order.setUser(user);
        order.setEvent(event);
        order.setRegistration(registration);
        order.setAmountInMinorUnits(49900L);
        order.setCurrency("INR");

        when(paymentGateway.parseWebhook("payload", "sig"))
                .thenReturn(new GatewayWebhookResult(
                        "evt_123",
                        "order.paid",
                        99L,
                        "order_test_123",
                        "pay_123",
                        null
                ));
        when(paymentOrderRepository.findById(99L)).thenReturn(Optional.of(order));
        when(qrCodeService.generateRegistrationQrCode(55L, 1L)).thenReturn("qr-verified-user");

        String result = paymentService.handleWebhook("payload", "sig");

        assertThat(result).isEqualTo("Payment processed");
        assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.PAID);
        assertThat(order.getProviderPaymentId()).isEqualTo("pay_123");
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(registration.getPaymentStatus()).isEqualTo(RegistrationPaymentStatus.PAID);
        assertThat(registration.getQrCode()).isEqualTo("qr-verified-user");
        verify(emailService).sendPaymentReceipt(user, event, order);
        verify(emailService).sendEventRegistrationConfirmation(user, event, registration);
        verify(paymentTransactionRepository).save(any());
    }

    @Test
    void refundOrderMarksOrderRefunded() {
        User admin = new User();
        admin.setId(2L);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        User student = studentUser();
        Event event = paidEvent();

        Registration registration = new Registration();
        registration.setId(55L);
        registration.setUser(student);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setPaymentRequired(true);
        registration.setPaymentStatus(RegistrationPaymentStatus.PAID);

        PaymentOrder order = new PaymentOrder();
        order.setId(99L);
        order.setStatus(PaymentOrderStatus.PAID);
        order.setUser(student);
        order.setEvent(event);
        order.setRegistration(registration);
        order.setProviderPaymentId("pay_123");
        order.setAmountInMinorUnits(49900L);
        order.setCurrency("INR");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", null)
        );

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(paymentOrderRepository.findById(99L)).thenReturn(Optional.of(order));
        when(paymentGateway.refundPayment(order, "Requested by student")).thenReturn("rfnd_123");

        RefundRequest request = new RefundRequest();
        request.setReason("Requested by student");

        var response = paymentService.refundOrder(99L, request);

        assertThat(response.getStatus()).isEqualTo(PaymentOrderStatus.REFUNDED);
        assertThat(registration.getPaymentStatus()).isEqualTo(RegistrationPaymentStatus.REFUNDED);
        verify(paymentTransactionRepository).save(any());
    }

    private User studentUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Student");
        user.setEmail("student@example.com");
        user.setRole(Role.STUDENT);
        return user;
    }

    private Event paidEvent() {
        Event event = new Event();
        event.setId(10L);
        event.setTitle("Paid Hackathon");
        event.setPaid(true);
        event.setPrice(499.0);
        return event;
    }
}
