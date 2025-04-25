package com.example.commonevents.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent {

    private Long orderId;
    private Integer amount;
    private Long buyerId;
    private Instant timestamp;
    private String paymentKey;
}