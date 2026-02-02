package com.vnpay.springboot.Repository;

import com.vnpay.springboot.Entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByTxnRef(String txnRef);
    boolean existsByTxnRef(String txnRef);
}
