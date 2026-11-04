package com.datlichkhambenh.backend;

import com.datlichkhambenh.backend.entity.NotificationOutbox;
import com.datlichkhambenh.backend.messaging.*;
import com.datlichkhambenh.backend.messaging.outbox.*;
import com.datlichkhambenh.backend.pattern.notification.NotificationSender;
import com.datlichkhambenh.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationDurabilityTests {
    @Autowired OutboxService outbox;
    @Autowired OutboxDispatcher dispatcher;
    @Autowired DurableNotificationConsumer consumer;
    @Autowired NotificationOutboxRepository notifications;
    @Autowired NotificationReceiptRepository receipts;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json;
    @MockBean OutboxTransport transport;
    @MockBean NotificationSender sender;

    @BeforeEach void clean() { notifications.deleteAll(); receipts.deleteAll(); reset(transport, sender); }
    private EmailNotificationMessage message() {
        return new EmailNotificationMessage("BOOKING_CONFIRMED", "patient@example.test", "Patient", "BK-OUTBOX",
                "Doctor", "2030-10-10", "09:00", "09:30", "300000");
    }
    @Test void rollbackDoesNotLeaveAnOutboxMessage() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            outbox.enqueue(message()); tx.setRollbackOnly();
        });
        assertEquals(0, notifications.count());
    }
    @Test void repeatedBusinessEventHasOneDurableMessage() {
        String first = outbox.enqueue(message());
        assertEquals(first, outbox.enqueue(message()));
        assertEquals(1, notifications.count());
        assertEquals("PENDING", notifications.findById(first).orElseThrow().getStatus());
    }
    @Test void failedPublishIsRetriedAndOnlyBrokerAcceptanceMarksPublished() {
        String id = outbox.enqueue(message());
        doThrow(new IllegalStateException("simulated broker unavailable")).when(transport).publish(anyString(), anyString(), anyString());
        assertEquals(1, dispatcher.dispatchBatch());
        NotificationOutbox failed = notifications.findById(id).orElseThrow();
        assertEquals("PENDING", failed.getStatus());
        assertEquals(1, failed.getAttempts());
        assertNull(failed.getPublishedAt());
        assertTrue(failed.getNextAttemptAt().isAfter(failed.getCreatedAt()));
        failed.setNextAttemptAt(LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1)); notifications.save(failed);
        reset(transport);
        assertEquals(1, dispatcher.dispatchBatch());
        assertEquals("SENT", notifications.findById(id).orElseThrow().getStatus());
    }
    @Test void exhaustedPublishAttemptsRemainDurablyDeadForOperations() {
        String id = outbox.enqueue(message());
        NotificationOutbox item = notifications.findById(id).orElseThrow(); item.setAttempts(7); notifications.save(item);
        doThrow(new IllegalStateException("offline")).when(transport).publish(anyString(), anyString(), anyString());
        dispatcher.dispatchBatch();
        assertEquals("DEAD", notifications.findById(id).orElseThrow().getStatus());
    }
    @Test void duplicateConsumerMessageSendsOnceUsingDurableReceipt() throws Exception {
        String payload = json.writeValueAsString(message());
        consumer.deliver("durable-message-1", "BOOKING", payload);
        consumer.deliver("durable-message-1", "BOOKING", payload);
        assertTrue(receipts.existsById("durable-message-1"));
        verify(sender, times(1)).send(eq("patient@example.test"), any());
    }
    @Test void smtpFailureRollsBackReceiptSoRetryCanSend() throws Exception {
        String payload = json.writeValueAsString(message());
        doThrow(new IllegalStateException("smtp offline")).when(sender).send(anyString(), any());
        assertThrows(IllegalStateException.class, () -> consumer.deliver("retry-message", "BOOKING", payload));
        assertFalse(receipts.existsById("retry-message"));
        reset(sender);
        consumer.deliver("retry-message", "BOOKING", payload);
        assertTrue(receipts.existsById("retry-message"));
        verify(sender).send(eq("patient@example.test"), any());
    }
    @Test void accountMessageUsesConfiguredPublicUrlAndDurableOutbox() {
        outbox.enqueueAccount(new AccountNotificationMessage("PASSWORD_RESET", "account@example.test",
                "https://clinic.example/reset-password.html#token=random"));
        assertEquals(1, notifications.count());
        assertEquals("ACCOUNT", notifications.findAll().getFirst().getKind());
    }
}
