package com.datlichkhambenh.backend.messaging.outbox;

import com.datlichkhambenh.backend.entity.NotificationOutbox;
import com.datlichkhambenh.backend.repository.NotificationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Row locks serialize dispatchers; crash after broker ACK can republish the same message ID. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcher {
    private final NotificationOutboxRepository notifications;
    private final OutboxTransport transport;
    private final MeterRegistry metrics;
    @Value("${app.delivery.max-publish-attempts:8}") private int maxAttempts;

    @Transactional
    public int dispatchBatch() {
        var due = notifications.findDueForUpdate(LocalDateTime.now(ZoneOffset.UTC), PageRequest.of(0, 20));
        for (NotificationOutbox item : due) {
            item.setAttempts(item.getAttempts() + 1);
            try {
                transport.publish(item.getId(), item.getKind(), item.getPayload());
                item.setStatus("SENT"); item.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC)); item.setLastErrorCode(null);
                metrics.counter("clinic.notifications.publish", "result", "accepted").increment();
            } catch (RuntimeException ex) {
                item.setLastErrorCode(ex.getClass().getSimpleName());
                if (item.getAttempts() >= maxAttempts) item.setStatus("DEAD");
                item.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(
                        Math.min(3600L, 1L << Math.min(item.getAttempts(), 12))));
                metrics.counter("clinic.notifications.publish", "result", "failed").increment();
                // Do not log exception messages, email payloads, recipients or clinical data.
                log.warn("Notification publish failed id={} attempt={} code={}",
                        item.getId(), item.getAttempts(), item.getLastErrorCode());
            }
        }
        return due.size();
    }
}
