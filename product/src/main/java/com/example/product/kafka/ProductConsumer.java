package com.example.product.kafka;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.commonevents.order.OrderedProductDto;
import com.example.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-created-topic", groupId = "order-created-group")
    public void handleStockCheckProcess(OrderCreatedEvent event) {
        log.info("Received Kafka message: {}", event);
        productService.checkStock(event);
    }

    @KafkaListener(topics = "order-restore-topic", groupId = "order-restore-group")
    public void handleStockResotre(List<Object> list) {
        log.info("Received Kafka message: {}", list);

        List<OrderedProductDto> productList = list.stream()
                        .map(obj -> objectMapper.convertValue(obj, OrderedProductDto.class))
                                .toList();
        productList.forEach(productService::restoreStock);
    }
}
