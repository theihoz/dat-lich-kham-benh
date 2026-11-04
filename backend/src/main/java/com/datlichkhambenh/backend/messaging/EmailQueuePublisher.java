package com.datlichkhambenh.backend.messaging;

import com.datlichkhambenh.backend.messaging.outbox.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

/** Observer wakes delivery after commit; the scheduled scan recovers any missed wakeup. */
@Component
@Profile("!demo & !test")
@RequiredArgsConstructor
public class EmailQueuePublisher {
    private final OutboxDispatcher dispatcher;
    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(EmailNotificationRequestedEvent event) { dispatcher.dispatchBatch(); }
    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void accountAfterCommit(OutboxWakeupEvent event) { dispatcher.dispatchBatch(); }
}
