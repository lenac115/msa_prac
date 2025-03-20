package com.example.payment.controller;

import com.example.payment.dto.PaymentConfirmRequest;
import com.example.payment.dto.PaymentDto;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable Long paymentId) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(paymentService.getPayment(paymentId));
    }

    @PostMapping("/delete/{paymentId}")
    public ResponseEntity<Object> deletePayment(@PathVariable Long paymentId) {

        paymentService.cancel(paymentId);
        return ResponseEntity.status(HttpStatus.OK)
                .body("취소 완료");
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmRequest request) throws IOException {
        paymentService.processPaymentRequest(request);
        return ResponseEntity.ok().body("Payment Successful");
    }
}
