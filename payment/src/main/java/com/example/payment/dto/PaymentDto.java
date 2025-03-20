package com.example.payment.dto;


import com.example.payment.domain.Payment;
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
    private Long orderId;  // 주문 ID
    private Long buyerId; // 구매자 이메일
    private Integer amount; // 결제 금액
    private Payment.Status status; // 결제 상태 (PENDING, COMPLETED, FAILED)
    private LocalDateTime createdAt;
    private String paymentKey;
}
