package com.datlichkhambenh.backend.messaging.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@Profile("!demo")
@RequiredArgsConstructor
public class RabbitOutboxTransport implements OutboxTransport {
    private final RabbitTemplate rabbit;
    @Value("${app.messaging.email-exchange:clinic.email.exchange}") private String exchange = "clinic.email.exchange";
    @Value("${app.messaging.email-routing-key:clinic.email.send}") private String routingKey = "clinic.email.send";
    @Value("${app.delivery.confirm-timeout-seconds:5}") private long timeoutSeconds = 5;

    @Override
    public void publish(String messageId, String kind, String payload) {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(messageId);
        properties.setHeader("notificationKind", kind);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        CorrelationData correlation = new CorrelationData(messageId);
        try {
            rabbit.send(exchange, routingKey, new Message(payload.getBytes(StandardCharsets.UTF_8), properties), correlation);
            var confirm = correlation.getFuture().get(timeoutSeconds, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("Broker did not route and confirm notification");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for broker confirmation");
        } catch (Exception ex) {
            // The payload and broker exception may contain sensitive metadata; expose a fixed message.
            throw new IllegalStateException("Notification publish was not confirmed");
        }
    }
}
