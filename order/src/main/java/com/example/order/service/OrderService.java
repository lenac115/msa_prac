package com.example.order.service;

import com.example.commonevents.order.*;
import com.example.commonevents.payment.PaymentCompletedEvent;
import com.example.commonevents.payment.StockRestoreEvent;
import com.example.exception.CustomException;
import com.example.exception.errorcode.OrderErrorCode;
import com.example.order.client.AuthServiceClient;
import com.example.order.domain.Order;
import com.example.order.domain.OrderedProduct;
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

    public OrderDto createOrder(List<NewOrder> NewOrders, String authorizationHeader) {
        Order created = Order.builder()
                .status(Status.PENDING)
                .buyerId(authServiceClient.getBuyerId(authorizationHeader))
                .build();

        for (NewOrder newOrder : NewOrders) {
            OrderedProduct orderedProduct = OrderedProduct.builder()
                    .productId(newOrder.getProductId())
                    .quantity(newOrder.getQuantity())
                    .price(newOrder.getAmount() / newOrder.getQuantity())
                    .order(created)
                    .build();
            created.getOrderedProductList().add(orderedProduct);
            orderedProductRepository.save(orderedProduct);
        }

        Order createdOrder = orderRepository.save(created);

        orderProducer.sendOrderCreatedEvent(OrderCreatedEvent.builder()
                .orderId(created.getId())
                .buyerId(created.getBuyerId())
                .orderedProducts(created.getOrderedProductList().stream().map(this::convertToDto).collect(Collectors.toList()))
                .timestamp(Instant.now())
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CREATED")
                .build());
        return convertToDto(createdOrder);
    }

    public void processOrderFailure(PaymentCompletedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.FAILED);
    }

    public void processOrderFailure(OrderFailedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.FAILED);
    }

    public void processOrderSuccess(PaymentCompletedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.PAID);
    }

    public void cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.CANCELED);
        orderProducer.sendOrderCancelledEvent(OrderCancelledEvent.builder()
                .orderedProducts(order.getOrderedProductList().stream().map(this::convertToDto).collect(Collectors.toList()))
                .timestamp(Instant.now())
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CANCELLED")
                .build());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        return convertToDto(order);
    }

    public void productStockRestore(StockRestoreEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        orderProducer.sendProductRestore(order.getOrderedProductList().stream().map(this::convertToDto).collect(Collectors.toList()));
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
