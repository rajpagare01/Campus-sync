package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class RegistrationResponse {
    private Long id;
    private String userName;
    private String eventTitle;
    private Long eventId;
    private String status;
    private String paymentStatus;
    private boolean paymentRequired;
    private Long paymentOrderId;
    private String providerOrderId;
    private String razorpayKeyId;
    private Long amountInMinorUnits;
    private String currency;
    private boolean alreadyRegistered;
    private boolean canCancel;
    private String qrCode;
}
