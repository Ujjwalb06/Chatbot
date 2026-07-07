package com.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset - AI Chatbot");
        message.setText(
            "Hi,\n\n"
            + "You requested a password reset.\n\n"
            + "Click the link below (expires in 15 minutes):\n"
            + resetLink + "\n\n"
            + "If you didn't request this, please ignore this email.\n\n"
            + "- AI Chatbot Team"
        );
        mailSender.send(message);
        log.info("Reset email sent to {}", toEmail);
    }
}