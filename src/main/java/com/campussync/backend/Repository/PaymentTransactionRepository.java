package com.campussync.backend.Repository;

import com.campussync.backend.Model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    boolean existsByGatewayEventId(String gatewayEventId);
}
