package com.example.order.test;

import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderedProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("test")
@RestController
@RequestMapping("/order/public")
@RequiredArgsConstructor
public class TestResetController {

    private final OrderRepository orderRepository;
    private final OrderedProductRepository orderedProductRepository;

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        orderedProductRepository.deleteAll();
        orderRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}