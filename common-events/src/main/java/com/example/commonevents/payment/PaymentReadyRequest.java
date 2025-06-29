package com.example.commonevents.payment;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentReadyRequest {

    private String paymentKey;
    private String orderEventId;
    private Integer amount;
    private Status status;
}
