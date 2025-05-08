package com.example.commonevents.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentConfirmRequest {

    private String payToken;
    private Long orderId;
    private Integer totalAmount;
}
