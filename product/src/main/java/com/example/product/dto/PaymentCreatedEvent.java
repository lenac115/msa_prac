package com.example.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreatedEvent {

    private String eventId;
    private String eventType;
    private Long orderId;
    private Integer amount;
    private Long buyerId;
    private Instant timestamp;
}
