package com.campussync.backend.Service;

public record GatewayWebhookResult(
        String eventId,
        String type,
        Long orderId,
        String providerOrderId,
        String providerPaymentId,
        String failureMessage
) {
}
