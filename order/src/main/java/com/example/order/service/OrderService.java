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
import java.util.*;
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
                .orderEventId(UUID.randomUUID().toString())
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

        Order order = orderRepository.findByOrderEventId(event.getOrderEventId()).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.FAILED);
    }

    public void processOrderFailure(OrderFailedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.FAILED);
    }

    public void processOrderSuccess(PaymentCompletedEvent event) {

        Order order = orderRepository.findByOrderEventId(event.getOrderEventId()).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.PAID);
    }

    public void cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.CANCELED);
        orderProducer.sendOrderCancelEvent(OrderCancelEvent.builder()
                        .orderEventId(order.getOrderEventId())
                        .eventId(UUID.randomUUID().toString())
                        .eventType("ORDER_CANCEL")
                        .timestamp(Instant.now())
                .build());
        orderProducer.sendProductRestore(order.getOrderedProductList().stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    public void cancelOrder(String orderEventId) {

        Order order = orderRepository.findByOrderEventId(orderEventId).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        order.updateStatus(Status.CANCELED);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        return convertToDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(String orderEventId) {
        Order order = orderRepository.findByOrderEventId(orderEventId).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        return convertToDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderedProductDto> getOrderedProducts(Long orderId) {
        return Optional.of(orderedProductRepository.findAllByOrderId(orderId).stream()
                        .map(this::convertToDto)
                .toList()).orElseGet(Collections::emptyList);
    }

    public void productStockRestore(StockRestoreEvent event) {

        Order order = orderRepository.findByOrderEventId(event.getOrderEventId()).orElseThrow(() -> new CustomException(OrderErrorCode.NOT_EXISTS_ORDER));
        System.out.println(order.getOrderedProductList());
        orderProducer.sendProductRestore(order.getOrderedProductList().stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<OrderDtoWithProdId> getOrderListByUserId(Long userId) {

        return Optional.of(orderRepository.findAllByBuyerId(userId).stream()
                .map(this::convertWithProdId)
                .toList()).orElseGet(Collections::emptyList);
    }

    private OrderDtoWithProdId convertWithProdId(Order order) {
        return OrderDtoWithProdId.builder()
                .buyerId(order.getBuyerId())
                .orderEventId(order.getOrderEventId())
                .productId(order.getOrderedProductList().stream()
                        .map(OrderedProduct::getProductId)
                        .toList())
                .id(order.getId())
                .status(order.getStatus())
                .build();
    }

    private OrderDto convertToDto(Order order) {
        return OrderDto.builder()
                .orderEventId(order.getOrderEventId())
                .id(order.getId())
                .status(order.getStatus())
                .buyerId(order.getBuyerId())
                .build();
    }

    private OrderedProductDto convertToDto(OrderedProduct orderedProduct) {
        return OrderedProductDto.builder()
                .orderId(orderedProduct.getOrder().getId())
                .price(orderedProduct.getPrice())
                .quantity(orderedProduct.getQuantity())
                .productId(orderedProduct.getProductId())
                .id(orderedProduct.getId())
                .build();
    }
}
