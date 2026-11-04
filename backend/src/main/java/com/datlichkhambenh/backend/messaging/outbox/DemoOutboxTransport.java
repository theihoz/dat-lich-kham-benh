package com.datlichkhambenh.backend.messaging.outbox;

import com.datlichkhambenh.backend.messaging.DurableNotificationConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoOutboxTransport implements OutboxTransport {
    private final DurableNotificationConsumer consumer;
    public void publish(String messageId, String kind, String payload) { consumer.deliver(messageId, kind, payload); }
}
