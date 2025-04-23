package com.example.payment.test;

import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("test")
@RestController
@RequestMapping("/payment/public")
@RequiredArgsConstructor
public class TestResetController {

    private final PaymentRepository paymentRepository;

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        paymentRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}