package com.example.payment.kafka;

import com.example.commonevents.payment.PaymentConfirmRequest;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-payment-topic", groupId = "payment-group")
    public void handlePayment(PaymentConfirmRequest request) throws IOException {
        log.info("Received Kafka message: {}", request);
        paymentService.processPaymentRequest(request);
    }
}
