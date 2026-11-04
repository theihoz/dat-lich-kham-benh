package com.datlichkhambenh.backend.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class EmailQueueConsumer {
    private final DurableNotificationConsumer consumer;

    @RabbitListener(queues = "${app.messaging.email-queue}")
    public void consume(Message message) {
        consumer.deliver(message.getMessageProperties().getMessageId(),
                message.getMessageProperties().getHeader("notificationKind"),
                new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
