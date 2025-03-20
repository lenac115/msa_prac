package com.example.payment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long buyerId;
    private Integer amount;
    private String paymentKey;


    @Enumerated(EnumType.STRING)
    private Payment.Status status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Status {
        PENDING,
        PAID,
        FAILED,
        CANCELED
    }

    public void cancel() {
        this.status = Payment.Status.CANCELED;
    }

    public void updateStatus(Payment.Status status) {
        this.status = status;
    }
}
