package com.example.order.kafka;

import com.example.commonevents.order.OrderCancelEvent;
import com.example.commonevents.order.OrderCreatedEvent;
import com.example.commonevents.order.OrderDto;
import com.example.commonevents.order.OrderedProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        kafkaTemplate.send("order-created-topic", event);
    }

    public void sendProductRestore(List<OrderedProductDto> event) {
        kafkaTemplate.send("order-restore-topic", event);
    }

    public void sendOrderCancelEvent(OrderCancelEvent event) {
        kafkaTemplate.send("order-restore-topic", event);
    }
}
