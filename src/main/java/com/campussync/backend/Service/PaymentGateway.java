package com.campussync.backend.Service;

import com.campussync.backend.Model.PaymentOrder;

public interface PaymentGateway {
    GatewayCheckoutSession createCheckoutSession(PaymentOrder order, String successUrl, String cancelUrl);
    GatewayWebhookResult parseWebhook(String payload, String signatureHeader);
    String refundPayment(PaymentOrder order, String reason);
}
