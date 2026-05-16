package com.tiptracker.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetLink = "https://tiptrackerapp.org/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Reset your TipTracker password");
            message.setText(
                    "You requested a password reset for your TipTracker account.\n\n" +
                    "Click the link below to set a new password:\n" +
                    resetLink + "\n\n" +
                    "This link expires in 1 hour.\n\n" +
                    "If you did not request this, you can safely ignore this email."
            );

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
