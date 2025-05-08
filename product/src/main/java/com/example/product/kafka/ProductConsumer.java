package com.example.product.kafka;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.commonevents.order.OrderedProductDto;
import com.example.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @KafkaListener(topics = "order-restore-topic", groupId = "order-restore-group")
    public void handleStockResotre(List<OrderedProductDto> list) {
        log.info("Received Kafka message: {}", list);
        list.forEach(productService::restoreStock);
    }
}
