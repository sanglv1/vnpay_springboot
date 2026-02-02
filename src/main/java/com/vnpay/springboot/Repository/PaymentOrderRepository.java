package com.vnpay.springboot.Repository;

import com.vnpay.springboot.Entity.PaymentOrder;
import com.vnpay.springboot.Entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByTxnRef(String txnRef);
    boolean existsByTxnRef(String txnRef);

    Page<PaymentOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<PaymentOrder> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);
    Page<PaymentOrder> findByTxnRefContainingOrderByCreatedAtDesc(String txnRef, Pageable pageable);
    Page<PaymentOrder> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countByStatus(PaymentStatus status);
}
