package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class PaymentCheckoutResponse {
    private Long orderId;
    private Long registrationId;
    private String providerOrderId;
    private String razorpayKeyId;
    private Long amountInMinorUnits;
    private String currency;
    private String paymentStatus;
}
