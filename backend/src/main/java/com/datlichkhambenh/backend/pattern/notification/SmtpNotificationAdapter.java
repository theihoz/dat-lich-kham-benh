package com.datlichkhambenh.backend.pattern.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!demo")
@RequiredArgsConstructor
public class SmtpNotificationAdapter implements NotificationSender {
    private final JavaMailSender mailSender;
    @Override public void send(String recipient, NotificationContent content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setSubject(content.subject());
        message.setText(content.body());
        mailSender.send(message);
    }
}
