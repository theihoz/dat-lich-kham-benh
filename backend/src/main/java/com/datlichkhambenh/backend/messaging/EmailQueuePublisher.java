package com.datlichkhambenh.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@Profile("!demo")
@RequiredArgsConstructor
public class EmailQueuePublisher {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.messaging.email-exchange}")
    private String exchange;

    @Value("${app.messaging.email-routing-key}")
    private String routingKey;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(EmailNotificationRequestedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event.message());
            log.info("Queued email notification for booking {}", event.message().bookingCode());
        } catch (Exception ex) {
            log.error("Cannot queue email notification for booking {}: {}",
                    event.message().bookingCode(), ex.getMessage());
        }
    }
}
