package com.example.product.kafka;

import com.example.product.dto.OrderFailedEvent;
import com.example.product.dto.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderFailedEvent(OrderFailedEvent event) {
        log.info("send Order Failed event: {}", event);
        kafkaTemplate.send("order-failed-topic", event);
    }

    public void sendPaymentCreated(PaymentCreatedEvent event) {
        log.info("send Payment Created event: {}", event);
        kafkaTemplate.send("payment-created-topic", event);
    }
}
