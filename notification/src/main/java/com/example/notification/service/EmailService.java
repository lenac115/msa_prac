package com.example.notification.service;

import com.example.exception.CustomException;
import com.example.exception.errorcode.NotificationErrorCode;
import com.example.notification.domain.Notification;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final NotificationRepository notificationRepository;

    public void sendResetPasswordEmail(String email, String token) {
        String resetLink = "https://localhost:3000/user/reset-password-page?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("비밀번호 재설정 요청");
        message.setText("비밀번호를 재설정하려면 아래 링크를 클릭하세요:\n" + resetLink);

        try {
            javaMailSender.send(message);

            // 전송 성공 로깅
            notificationRepository.save(Notification.builder()
                    .recipientEmail(email)
                    .content(message.getText())
                    .sentAt(LocalDateTime.now())
                    .subject(message.getSubject())
                    .success(true)
                    .failureReason(null)
                    .build());
        } catch (MailException e) {
            // 전송 실패 로깅
            notificationRepository.save(Notification.builder()
                    .recipientEmail(email)
                    .content(message.getText())
                    .sentAt(LocalDateTime.now())
                    .subject(message.getSubject())
                    .success(false)
                    .failureReason(e.getMessage())
                    .build());

            throw new CustomException(NotificationErrorCode.SEND_MAIL_FAILURE);
        }
    }

}