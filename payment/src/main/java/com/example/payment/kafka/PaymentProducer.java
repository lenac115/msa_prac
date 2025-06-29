package com.example.payment.kafka;

import com.example.commonevents.payment.PaymentCompletedEvent;
import com.example.commonevents.payment.PaymentDto;
import com.example.commonevents.payment.StockRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentSuccess(PaymentDto paymentDto) {
        log.info("Sending payment success event to Kafka: {}", paymentDto);
        kafkaTemplate.send("payment-success-topic", PaymentCompletedEvent.builder()
                        .paymentId(paymentDto.getPaymentKey())
                        .eventId(UUID.randomUUID().toString())
                        .orderEventId(paymentDto.getOrderEventId())
                        .timestamp(Instant.now())
                        .eventType("PAYMENT_SUCCESS")
                .build());
    }

    public void sendPaymentFailure(PaymentDto paymentDto) {
        log.info("Sending payment failure event to Kafka: {}", paymentDto);
        kafkaTemplate.send("payment-failure-topic", PaymentCompletedEvent.builder()
                .paymentId(paymentDto.getPaymentKey())
                .eventId(UUID.randomUUID().toString())
                .orderEventId(paymentDto.getOrderEventId())
                .timestamp(Instant.now())
                .eventType("PAYMENT_FAILURE")
                .build());
    }

    public void sendStockRestore(StockRestoreEvent stockRestore) {
        log.info("Sending stock restore event to Kafka: {}", stockRestore);
        kafkaTemplate.send("stock-restore-topic", stockRestore);
    }
}
