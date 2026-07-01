package com.chatbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Reads from spring.mail.username in application.properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);   // ✅ FIXED: Gmail SMTP requires From to match authenticated account
        message.setTo(toEmail);
        message.setSubject("Password Reset - My Chatbot");
        message.setText("Hi,\n\n"
                + "You requested a password reset for your Chatbot account.\n\n"
                + "Click the link below to reset your password:\n"
                + resetLink + "\n\n"
                + "This link expires in 15 minutes.\n\n"
                + "If you didn't request this, please ignore this email.\n\n"
                + "- Chatbot Team");

        mailSender.send(message);
    }
}