package com.campussync.backend.Repository;

import com.campussync.backend.Model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByProviderOrderId(String providerOrderId);
    Optional<PaymentOrder> findByRegistrationId(Long registrationId);
    Optional<PaymentOrder> findByClientRequestIdAndUserId(String clientRequestId, Long userId);
    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
