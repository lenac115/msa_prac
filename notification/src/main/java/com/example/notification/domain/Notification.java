package com.example.notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue
    private Long id;

    private String recipientEmail;
    private String subject;

    @Lob
    private String content;

    private boolean success;
    private String failureReason;
    private LocalDateTime sentAt;

    public static Notification createSuccessLog(String to, String subject, String content) {
        Notification log = new Notification();
        log.recipientEmail = to;
        log.subject = subject;
        log.content = content;
        log.success = true;
        log.sentAt = LocalDateTime.now();
        return log;
    }

    public static Notification createFailLog(String to, String subject, String reason) {
        Notification log = new Notification();
        log.recipientEmail = to;
        log.subject = subject;
        log.success = false;
        log.failureReason = reason;
        log.sentAt = LocalDateTime.now();
        return log;
    }
}