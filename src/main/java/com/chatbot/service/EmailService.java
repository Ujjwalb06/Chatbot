package com.chatbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset - My Chatbot");
        message.setText("You requested a password reset.\n\n"
                + "Click the link below to reset your password:\n"
                + resetLink + "\n\n"
                + "This link expires in 15 minutes.\n\n"
                + "If you didn't request this, please ignore this email.");
        mailSender.send(message);
    }
}