package com.datlichkhambenh.backend.demo;

import com.datlichkhambenh.backend.messaging.EmailNotificationRequestedEvent;
import com.datlichkhambenh.backend.messaging.outbox.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

/** Same after-commit Observer and durable outbox; the demo transport writes only to its local inbox. */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoNotificationObserver {
    private final OutboxDispatcher dispatcher;
    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(EmailNotificationRequestedEvent event) { dispatcher.dispatchBatch(); }
    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void accountAfterCommit(OutboxWakeupEvent event) { dispatcher.dispatchBatch(); }
}
