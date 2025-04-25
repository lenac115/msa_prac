package com.example.product.kafka;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "order-created-topic", groupId = "order-created-group")
    public void handleStockCheckProcess(OrderCreatedEvent event) {
        log.info("Received Kafka message: {}", event);
        productService.checkStock(event);
    }
}
