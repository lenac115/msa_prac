package com.example.payment.controller;

import com.example.commonevents.payment.PaymentConfirmRequest;
import com.example.commonevents.payment.PaymentDto;
import com.example.commonevents.payment.PaymentReadyRequest;
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

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDto> getPaymentByOrderId(@PathVariable String orderId) {
        PaymentDto paymentDto = paymentService.getPaymentByOrderId(orderId);
        System.out.println(paymentDto.getPaymentKey());
        return ResponseEntity.status(HttpStatus.OK)
                .body(paymentDto);
    }

    @PostMapping("/delete/{paymentId}")
    public ResponseEntity<Object> deletePayment(@PathVariable String paymentId) {

        paymentService.cancel(paymentId);
        return ResponseEntity.status(HttpStatus.OK)
                .body("취소 완료");
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmRequest request) throws IOException {
        System.out.println("paymentConfirm");
        System.out.println(request);
        paymentService.processPaymentRequest(request);
        return ResponseEntity.ok().body("Payment Successful");
    }

    @PostMapping("/ready")
    public ResponseEntity<?> readyPayment(@RequestBody PaymentReadyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }
}
