package com.example.payment.service;

import com.example.payment.dto.*;
import com.example.payment.domain.Payment;
import com.example.payment.kafka.PaymentProducer;
import com.example.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    public PaymentDto getPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        return convertPaymentDto(payment);
    }

    @Transactional
    public void createPayment(PaymentCreatedEvent event) {

        Payment payment = Payment.builder()
                .amount(event.getAmount())
                .orderId(event.getOrderId())
                .createdAt(LocalDateTime.now())
                .buyerId(event.getBuyerId())
                .status(Payment.Status.PENDING)
                .build();
        paymentRepository.save(payment);
    }

    @Transactional
    public void processPaymentRequest(PaymentConfirmRequest request) throws IOException {
        // 토스페이먼츠 결제 승인 API 호출
        Payment payment = sendToTossPayments(request);

        // 결제 상태에 따라 처리 분기
        if (payment.getStatus() == Payment.Status.PAID) {
            processPaymentSuccess(payment);
        } else if (payment.getStatus() == Payment.Status.FAILED) {
            processPaymentFailure(payment);
        }
    }

    private Payment sendToTossPayments(PaymentConfirmRequest request) throws IOException {

        String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        String authorizationHeader = "Basic " + encodedAuth;

        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorizationHeader);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(request);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        try (InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();
             Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {

            if (responseCode >= 200 && responseCode < 300) {
                return objectMapper.readValue(reader, Payment.class);
            } else {
                ErrorResponse errorResponse = objectMapper.readValue(reader, ErrorResponse.class);
                throw new IOException("결제 승인 실패: " + errorResponse.getMessage());
            }
        }
    }

    @Transactional
    public void processPaymentSuccess(Payment payment) {
        payment.updateStatus(Payment.Status.PAID);

        paymentProducer.sendPaymentSuccess(convertPaymentDto(payment));

        log.info("결제 성공: 주문 ID={}, 결제 키={}", payment.getOrderId(), payment.getPaymentKey());
    }


    @Transactional
    public void processPaymentFailure(Payment payment) {
        payment.updateStatus(Payment.Status.FAILED);

        paymentProducer.sendPaymentFailure(convertPaymentDto(payment));

        log.info("결제 실패: 주문 ID={}", payment.getOrderId());
    }


    @Transactional
    public void cancel(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        payment.cancel();
        paymentProducer.sendStockRestore(StockRestoreEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(payment.getOrderId())
                .eventType("STOCK_RESTORE")
                .build());
    }

    private Payment convertPayment(PaymentDto paymentDto) {
        return Payment.builder()
                .amount(paymentDto.getAmount())
                .buyerId(paymentDto.getBuyerId())
                .createdAt(paymentDto.getCreatedAt())
                .id(paymentDto.getId())
                .orderId(paymentDto.getOrderId())
                .status(paymentDto.getStatus())
                .paymentKey(paymentDto.getPaymentKey())
                .build();
    }

    private PaymentDto convertPaymentDto(Payment payment) {
        return PaymentDto.builder()
                .amount(payment.getAmount())
                .buyerId(payment.getBuyerId())
                .createdAt(payment.getCreatedAt())
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .status(payment.getStatus())
                .paymentKey(payment.getPaymentKey())
                .build();
    }
}
