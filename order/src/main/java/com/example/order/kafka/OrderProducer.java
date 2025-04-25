package com.example.order.kafka;

import com.example.commonevents.order.OrderCancelledEvent;
import com.example.commonevents.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        kafkaTemplate.send("order-created-topic", event);
    }

    public void sendOrderCancelledEvent(OrderCancelledEvent event) {
        kafkaTemplate.send("order-cancelled-topic", event);
    }
}
