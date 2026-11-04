package com.datlichkhambenh.backend.messaging;

import com.datlichkhambenh.backend.messaging.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account emails share the durable notification outbox; tokens never enter application logs. */
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {
    private final OutboxService outbox;
    @Value("${app.public-url:http://localhost:8080}") private String publicUrl;

    @Transactional
    public void enqueueAccountMessage(String type, String email, String token) {
        String path = switch (type) {
            case "PASSWORD_RESET" -> "/reset-password.html";
            case "EMAIL_VERIFICATION" -> "/verify-email.html";
            default -> throw new IllegalArgumentException("Unsupported account notification type");
        };
        outbox.enqueueAccount(new AccountNotificationMessage(type, email,
                publicUrl.replaceAll("/+$", "") + path + "#token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8)));
    }
}
