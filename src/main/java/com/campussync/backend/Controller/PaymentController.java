package com.campussync.backend.Controller;

import com.campussync.backend.Dto.PaymentCheckoutRequest;
import com.campussync.backend.Dto.PaymentCheckoutResponse;
import com.campussync.backend.Dto.PaymentOrderResponse;
import com.campussync.backend.Dto.RefundRequest;
import com.campussync.backend.Service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('STUDENT')")
    public PaymentCheckoutResponse createCheckoutSession(@Valid @RequestBody PaymentCheckoutRequest request,
                                                         @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        if ((request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) && idempotencyKey != null) {
            request.setIdempotencyKey(idempotencyKey);
        }
        return paymentService.createCheckoutSession(request);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signatureHeader) {
        String result = paymentService.handleWebhook(payload, signatureHeader);
        return ResponseEntity.ok(Map.of("status", result));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<PaymentOrderResponse> getMyOrders() {
        return paymentService.getMyOrders();
    }

    @GetMapping({"/orders/{orderId}", "/{orderId}"})
    @PreAuthorize("isAuthenticated()")
    public PaymentOrderResponse getOrder(@PathVariable Long orderId) {
        return paymentService.getOrder(orderId);
    }

    @PostMapping("/orders/{orderId}/refund")
    @PreAuthorize("isAuthenticated()")
    public PaymentOrderResponse refundOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) RefundRequest request) {
        return paymentService.refundOrder(orderId, request == null ? new RefundRequest() : request);
    }
}
