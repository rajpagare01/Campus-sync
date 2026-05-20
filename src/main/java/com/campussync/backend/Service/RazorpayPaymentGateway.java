package com.campussync.backend.Service;

import com.campussync.backend.Model.PaymentOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;
import org.json.JSONObject;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayPaymentGateway implements PaymentGateway {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String currency;
    private final ObjectMapper objectMapper;

    public RazorpayPaymentGateway(
            @Value("${payment.razorpay.key-id:}") String keyId,
            @Value("${payment.razorpay.key-secret:}") String keySecret,
            @Value("${payment.razorpay.webhook-secret:}") String webhookSecret,
            @Value("${payment.currency:INR}") String currency,
            ObjectMapper objectMapper) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.currency = currency;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "paymentGateway")
    @Retry(name = "paymentGateway")
    public GatewayCheckoutSession createCheckoutSession(PaymentOrder order, String successUrl, String cancelUrl) {
        ensureKeysConfigured();

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("amount", order.getAmountInMinorUnits());
            options.put("currency", currency.toUpperCase());
            options.put("receipt", "campussync_order_" + order.getId());
            JSONObject notes = new JSONObject();
            notes.put("orderId", String.valueOf(order.getId()));
            notes.put("registrationId", String.valueOf(order.getRegistration().getId()));
            notes.put("eventId", String.valueOf(order.getEvent().getId()));
            options.put("notes", notes);

            Order razorpayOrder = client.orders.create(options);
            return new GatewayCheckoutSession(razorpayOrder.get("id"), keyId);
        } catch (RazorpayException e) {
            throw new RuntimeException("Unable to create Razorpay order: " + e.getMessage());
        }
    }

    @Override
    public GatewayWebhookResult parseWebhook(String payload, String signatureHeader) {
        ensureWebhookConfigured();

        try {
            Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.path("id").asText(null);
            String eventType = root.path("event").asText();
            JsonNode payloadNode = root.path("payload");
            JsonNode paymentEntity = payloadNode.path("payment").path("entity");
            JsonNode orderEntity = payloadNode.path("order").path("entity");

            String providerOrderId = orderEntity.path("id").asText(null);
            String providerPaymentId = paymentEntity.path("id").asText(null);
            String notesOrderId = orderEntity.path("notes").path("orderId").asText(null);
            Long internalOrderId = notesOrderId == null || notesOrderId.isBlank() ? null : Long.parseLong(notesOrderId);
            String failureMessage = paymentEntity.path("error_description").asText(null);

            return new GatewayWebhookResult(
                    eventId,
                    eventType,
                    internalOrderId,
                    providerOrderId,
                    providerPaymentId,
                    failureMessage
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid Razorpay webhook: " + e.getMessage());
        }
    }

    @Override
    @CircuitBreaker(name = "paymentGateway")
    @Retry(name = "paymentGateway")
    public String refundPayment(PaymentOrder order, String reason) {
        ensureKeysConfigured();

        if (order.getProviderPaymentId() == null || order.getProviderPaymentId().isBlank()) {
            throw new RuntimeException("Razorpay payment id not found for refund");
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject request = new JSONObject();
            request.put("amount", order.getAmountInMinorUnits());
            request.put("notes", new JSONObject().put("reason", reason == null ? "" : reason));
            Refund refund = client.payments.refund(order.getProviderPaymentId(), request);
            return refund.get("id");
        } catch (RazorpayException e) {
            throw new RuntimeException("Unable to refund Razorpay payment: " + e.getMessage());
        }
    }

    private void ensureKeysConfigured() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new RuntimeException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
    }

    private void ensureWebhookConfigured() {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new RuntimeException("Razorpay webhook secret is not configured.");
        }
    }
}
