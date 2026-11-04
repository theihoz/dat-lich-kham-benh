package com.datlichkhambenh.backend.messaging.outbox;

import com.datlichkhambenh.backend.entity.NotificationOutbox;
import com.datlichkhambenh.backend.messaging.*;
import com.datlichkhambenh.backend.repository.NotificationOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Writes intent in the caller's transaction; no network calls occur here. */
@Service
@RequiredArgsConstructor
public class OutboxService {
    private final NotificationOutboxRepository notifications;
    private final ObjectMapper json;
    private final ApplicationEventPublisher events;

    @Transactional
    public String enqueue(EmailNotificationMessage message) {
        return enqueue("BOOKING", message.type() + ":" + message.bookingCode(), message);
    }
    @Transactional
    public String enqueueAccount(AccountNotificationMessage message) {
        String id = enqueue("ACCOUNT", message.type() + ":" + UUID.randomUUID(), message);
        events.publishEvent(new OutboxWakeupEvent());
        return id;
    }
    private String enqueue(String kind, String key, Object message) {
        return notifications.findByEventKey(key).map(NotificationOutbox::getId).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            try {
                NotificationOutbox item = NotificationOutbox.builder().id(UUID.randomUUID().toString())
                        .eventKey(key).kind(kind).payload(json.writeValueAsString(message)).status("PENDING")
                        .attempts(0).createdAt(now).nextAttemptAt(now).build();
                return notifications.saveAndFlush(item).getId();
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Cannot serialize notification", ex);
            }
        });
    }
}
