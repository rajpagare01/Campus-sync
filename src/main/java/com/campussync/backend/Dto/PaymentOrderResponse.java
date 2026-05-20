package com.campussync.backend.Dto;

import com.campussync.backend.Model.PaymentOrderStatus;
import com.campussync.backend.Model.PaymentProvider;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentOrderResponse {
    private Long id;
    private PaymentProvider provider;
    private PaymentOrderStatus status;
    private Long eventId;
    private String eventTitle;
    private Long registrationId;
    private Long amountInMinorUnits;
    private String currency;
    private String providerOrderId;
    private String providerPaymentId;
    private String checkoutPublicKey;
    private String failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
