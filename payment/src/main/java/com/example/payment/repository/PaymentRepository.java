package com.example.payment.repository;

import com.example.commonevents.payment.Status;
import com.example.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("select p from Payment p where p.orderEventId = :orderEventId and p.status = :status")
    Optional<Payment> findByOrderEventId(String orderEventId, Status status);

    @Query("select p from Payment p where p.paymentKey = :paymentKey")
    Optional<Payment> findByPaymentKey(String paymentKey);
}
