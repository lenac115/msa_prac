package com.example.payment.domain;

import com.example.commonevents.payment.Status;
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

    private String orderEventId;
    private Integer amount;
    private String paymentKey;

    private LocalDateTime approvedAt;


    @Enumerated(EnumType.STRING)
    private Status status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void cancel() {
        this.status = Status.CANCELED;
    }

    public void updateStatus(Status status, String payToken) {
        this.status = status;
        this.paymentKey = payToken;
    }
}
