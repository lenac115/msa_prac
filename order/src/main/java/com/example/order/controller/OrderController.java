package com.example.order.controller;

import com.example.commonevents.order.NewOrder;
import com.example.commonevents.order.OrderDto;
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

    @PostMapping("/buyer/create")
    public ResponseEntity<OrderDto> createOrder(@RequestBody List<NewOrder> orders,
                                              @RequestHeader("Authorization") String authorizationHeader) {
        OrderDto orderDto = orderService.createOrder(orders, authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDto);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable("orderId") Long orderId) {

        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrder(orderId));
    }
}
