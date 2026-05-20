package com.campussync.backend.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCheckoutRequest {
    @NotNull
    private Long eventId;
    private String idempotencyKey;
    private String successUrl;
    private String cancelUrl;
}
