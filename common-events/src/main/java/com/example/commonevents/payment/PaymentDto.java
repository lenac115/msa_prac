package com.example.commonevents.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    private Long id;
    private String orderEventId;  // 주문 ID
    private LocalDateTime approvedAt;
    private Integer amount; // 결제 금액
    private Status status; // 결제 상태 (PENDING, COMPLETED, FAILED)
    private LocalDateTime createdAt;
    private String paymentKey;
}
