package com.example.order.controller;

import com.example.order.dto.NewOrder;
import com.example.order.dto.OrderDto;
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
    public ResponseEntity<String> createOrder(@RequestBody List<NewOrder> orders,
                                              @RequestHeader("Authorization") String authorizationHeader) {
        orderService.createOrder(orders, authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body("생성 완료");
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable("orderId") Long orderId) {

        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrder(orderId));
    }
}
