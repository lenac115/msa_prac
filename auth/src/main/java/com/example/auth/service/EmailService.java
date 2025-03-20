package com.example.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendResetPasswordEmail(String email, String token) {
        String resetLink = "https://localhost:8080/user/reset-password-page?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("비밀번호 재설정 요청");
        message.setText("비밀번호를 재설정하려면 아래 링크를 클릭하세요:\n" + resetLink);

        javaMailSender.send(message);
    }
}
