package com.example.product.test;

import com.example.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("test")
@RestController
@RequestMapping("/product/public")
@RequiredArgsConstructor
public class TestResetController {

    private final ProductRepository productRepository;

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        productRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}