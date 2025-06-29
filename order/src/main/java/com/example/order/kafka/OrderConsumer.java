package com.example.order.kafka;

import com.example.commonevents.order.OrderFailedEvent;
import com.example.commonevents.payment.PaymentCompletedEvent;
import com.example.commonevents.payment.StockRestoreEvent;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-success-topic", groupId = "payment-success-group")
    public void handleSuccessPayment(PaymentCompletedEvent event) {
        log.info("Received Kafka message: {}", event);
        orderService.processOrderSuccess(event);
    }

    @KafkaListener(topics = "payment-failure-topic", groupId = "payment-failure-group")
    public void handleFailurePayment(PaymentCompletedEvent event) {
        log.info("Received Kafka message: {}", event);
        orderService.processOrderFailure(event);
    }

    @KafkaListener(topics = "order-failed-topic", groupId = "order-failed-group")
    public void handleOrderFailed(OrderFailedEvent event) {
        log.info("Received Kafka message: {}", event);
        orderService.processOrderFailure(event);
    }

    @KafkaListener(topics = "stock-restore-topic", groupId = "stock-restore-group")
    public void handleStockRestore(StockRestoreEvent event) {
        log.info("Received Kafka message: {}", event);
        orderService.cancelOrder(event.getOrderEventId());
        orderService.productStockRestore(event);
    }
}
