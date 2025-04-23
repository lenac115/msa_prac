package com.example.order.service;

import com.example.order.client.AuthServiceClient;
import com.example.order.domain.Order;
import com.example.order.domain.OrderedProduct;
import com.example.order.dto.*;
import com.example.order.kafka.OrderProducer;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderedProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderedProductRepository orderedProductRepository;
    private final AuthServiceClient authServiceClient;
    private final OrderProducer orderProducer;

    public void createOrder(List<NewOrder> NewOrders, String authorizationHeader) {
        Order created = Order.builder()
                .status(Order.Status.PENDING)
                .buyerId(authServiceClient.getBuyerId(authorizationHeader))
                .build();

        for (NewOrder newOrder : NewOrders) {
            OrderedProduct orderedProduct = OrderedProduct.builder()
                    .productId(newOrder.getProductId())
                    .quantity(newOrder.getQuantity())
                    .order(created)
                    .build();
            created.getOrderedProductList().add(orderedProduct);
            orderedProductRepository.save(orderedProduct);
        }

        orderRepository.save(created);

        orderProducer.sendOrderCreatedEvent(OrderCreatedEvent.builder()
                .orderId(created.getId())
                .buyerId(created.getBuyerId())
                .orderedProducts(created.getOrderedProductList().stream().map(this::convertToDto).collect(Collectors.toList()))
                .timestamp(Instant.now())
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CREATED")
                .build());
    }

    public void processOrderFailure(PaymentCompletedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new RuntimeException("존재하지 않는 주문"));
        order.updateStatus(Order.Status.FAILED);
    }

    public void processOrderFailure(OrderFailedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new RuntimeException("존재하지 않는 주문"));
        order.updateStatus(Order.Status.FAILED);
    }

    public void processOrderSuccess(PaymentCompletedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new RuntimeException("존재하지 않는 주문"));
        order.updateStatus(Order.Status.PAID);
    }

    public void cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("존재하지 않는 주문"));
        order.updateStatus(Order.Status.CANCELED);
        orderProducer.sendOrderCancelledEvent(OrderCancelledEvent.builder()
                .orderedProducts(order.getOrderedProductList().stream().map(this::convertToDto).collect(Collectors.toList()))
                .timestamp(Instant.now())
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CANCELLED")
                .build());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("존재하지 않는 주문"));
        return convertToDto(order);
    }

    private OrderDto convertToDto(Order order) {
        return OrderDto.builder()
                .id(order.getId())
                .status(order.getStatus())
                .buyerId(order.getBuyerId())
                .build();
    }

    private OrderedProductDto convertToDto(OrderedProduct orderedProduct) {
        return OrderedProductDto.builder()
                .price(orderedProduct.getPrice())
                .quantity(orderedProduct.getQuantity())
                .productId(orderedProduct.getProductId())
                .id(orderedProduct.getId())
                .build();
    }
}
