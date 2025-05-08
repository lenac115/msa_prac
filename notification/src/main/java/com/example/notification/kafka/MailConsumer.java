package com.example.notification.kafka;

import com.example.commonevents.auth.SendMailEvent;
import com.example.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class MailConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "reset-mail-topic", groupId = "mail-group")
    public void handleSendResetMail(SendMailEvent request) {
        log.info("Received Kafka message: {}", request);
        emailService.sendResetPasswordEmail(request.getEmail(), request.getToken());
    }
}
