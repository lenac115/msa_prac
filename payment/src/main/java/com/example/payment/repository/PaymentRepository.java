package com.example.payment.repository;

import com.example.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("select p from Payment p where p.orderId = :orderId and p.status = :status")
    Optional<Payment> findByOrderId(Long orderId, Payment.Status status);
}
