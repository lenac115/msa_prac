package com.example.payment.service;

import com.example.commonevents.payment.*;
import com.example.exception.CustomException;
import com.example.exception.errorcode.PaymentErrorCode;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    public PaymentDto getPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        return convertPaymentDto(payment);
    }

    public PaymentDto getPaymentByOrderId(String orderEventId) {
        Payment payment = paymentRepository.findByOrderEventId(orderEventId, Status.PENDING)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.NOT_EXIST_PAYMENT));
        return convertPaymentDto(payment);
    }

    @Transactional
    public void processPaymentRequest(PaymentConfirmRequest request) throws IOException {
        // 토스페이먼츠 결제 승인 API 호출
        //Payment payment = sendToTossPayments(request);

        Payment payment = mockingSendToTossPayments(request);

        // 결제 상태에 따라 처리 분기
        if (payment.getStatus() == Status.PAID) {
            processPaymentSuccess(payment);
        } else if (payment.getStatus() == Status.FAILED) {
            processPaymentFailure(payment);
        }
    }

    private Payment mockingSendToTossPayments(PaymentConfirmRequest request) throws IOException {
        Payment payment = paymentRepository.findByOrderEventId(request.getOrderEventId(), Status.PENDING)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.NOT_EXIST_PAYMENT));

        payment.updateStatus(Status.PAID, request.getPayToken());

        return payment;
    }

    private Payment sendToTossPayments(PaymentConfirmRequest request) throws IOException {

        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        String authorizationHeader = "Basic " + encodedAuth;

        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorizationHeader);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("paymentKey", request.getPayToken());
        requestMap.put("orderId", request.getOrderEventId());
        requestMap.put("amount", request.getTotalAmount());

        // 요청 바디 JSON 작성
        String requestBody = objectMapper.writeValueAsString(requestMap);


        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();

        log.info("Response Code: " + responseCode);
        System.out.println(request);
        Payment payment = paymentRepository.findByOrderEventId(request.getOrderEventId(), Status.PENDING)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.NOT_EXIST_PAYMENT));

        InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (responseStream == null) {
            log.error("Toss 응답 스트림이 null입니다. 응답 코드: {}", responseCode);
            payment.updateStatus(Status.FAILED, request.getPayToken());
            return payment;
        }

        try (Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
            if (responseCode >= 200 && responseCode < 300) {
                payment.updateStatus(Status.PAID, request.getPayToken());
            } else {
                payment.updateStatus(Status.FAILED, request.getPayToken());
                ErrorResponse errorResponse = objectMapper.readValue(reader, ErrorResponse.class);
                log.error("결제 검증 실패: {}", errorResponse.getMessage());
            }
        }

        return payment;
    }

    @Transactional
    public void processPaymentSuccess(Payment payment) {

        paymentProducer.sendPaymentSuccess(convertPaymentDto(payment));

        log.info("결제 성공: 주문 ID={}, 결제 키={}", payment.getOrderEventId(), payment.getPaymentKey());
    }


    @Transactional
    public void processPaymentFailure(Payment payment) {
        paymentProducer.sendPaymentFailure(convertPaymentDto(payment));
        log.info("결제 실패: 주문 ID={}", payment.getOrderEventId());
        throw new CustomException(PaymentErrorCode.FAILED_VALIDATION);
    }


    @Transactional
    public void cancel(String paymentKey) {

        Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElseThrow();
        payment.cancel();
        paymentProducer.sendStockRestore(StockRestoreEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderEventId(payment.getOrderEventId())
                .eventType("STOCK_RESTORE")
                .build());
    }

    @Transactional
    public PaymentDto createPayment(PaymentReadyRequest request) {
        System.out.println(request.getOrderEventId());
        Payment payment = Payment.builder()
                .paymentKey(request.getPaymentKey())
                .amount(request.getAmount())
                .status(Status.PENDING)
                .orderEventId(request.getOrderEventId())
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .build();

        return convertPaymentDto(paymentRepository.save(payment));
    }

    private Payment convertPayment(PaymentDto paymentDto) {
        return Payment.builder()
                .amount(paymentDto.getAmount())
                .createdAt(paymentDto.getCreatedAt())
                .id(paymentDto.getId())
                .approvedAt(paymentDto.getApprovedAt())
                .orderEventId(paymentDto.getOrderEventId())
                .status(paymentDto.getStatus())
                .paymentKey(paymentDto.getPaymentKey())
                .build();
    }

    private PaymentDto convertPaymentDto(Payment payment) {
        return PaymentDto.builder()
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .approvedAt(payment.getApprovedAt())
                .id(payment.getId())
                .orderEventId(payment.getOrderEventId())
                .status(payment.getStatus())
                .paymentKey(payment.getPaymentKey())
                .build();
    }
}
