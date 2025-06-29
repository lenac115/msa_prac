package com.example.order.controller;

import com.example.commonevents.order.NewOrder;
import com.example.commonevents.order.OrderDto;
import com.example.commonevents.order.OrderDtoWithProdId;
import com.example.commonevents.order.OrderedProductDto;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/common/cancel")
    public ResponseEntity<String> cancelOrder(@RequestParam("orderId") Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.status(HttpStatus.OK).body("삭제 완료");
    }

    @PostMapping("/common/create")
    public ResponseEntity<OrderDto> createOrder(@RequestBody List<NewOrder> orders,
                                              @RequestHeader("Authorization") String authorizationHeader) {
        OrderDto orderDto = orderService.createOrder(orders, authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDto);
    }

    @GetMapping("/common/get/list/{userId}")
    public ResponseEntity<List<OrderDtoWithProdId>> getOrderList(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrderListByUserId(userId));
    }

    @GetMapping("/common/get/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrder(orderId));
    }

    @GetMapping("/common/get")
    public ResponseEntity<OrderDto> getOrderUsingOrderEventId(@RequestParam String orderEventId) {

        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrder(orderEventId));
    }

    @GetMapping("/common/get/orderItem/{orderId}")
    public ResponseEntity<List<OrderedProductDto>> getOrderItem(@PathVariable("orderId") Long orderId) {

        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrderedProducts(orderId));
    }
}
