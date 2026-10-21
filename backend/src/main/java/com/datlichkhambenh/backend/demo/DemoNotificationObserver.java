package com.datlichkhambenh.backend.demo;

import com.datlichkhambenh.backend.messaging.EmailNotificationRequestedEvent;
import com.datlichkhambenh.backend.messaging.EmailQueueConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Observer thay cầu nối RabbitMQ bằng xử lý cục bộ khi trình bày trên lớp. */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoNotificationObserver {
    private final EmailQueueConsumer consumer;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(EmailNotificationRequestedEvent event) {
        consumer.consume(event.message());
    }
}
