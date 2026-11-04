package com.datlichkhambenh.backend.service;

import com.datlichkhambenh.backend.messaging.outbox.RabbitOutboxTransport;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxPublisherConfirmTests {
    @Test void publishingWaitsForPositiveConfirmAndCarriesStableId() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doAnswer(call -> {
            Message sent = call.getArgument(2);
            assertEquals("message-42", sent.getMessageProperties().getMessageId());
            assertEquals("BOOKING", sent.getMessageProperties().getHeader("notificationKind"));
            CorrelationData correlation = call.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertDoesNotThrow(() -> new RabbitOutboxTransport(rabbit).publish("message-42", "BOOKING", "{}"));
    }
    @Test void negativeConfirmIsAnUnsuccessfulPublish() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doAnswer(call -> {
            CorrelationData correlation = call.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "rejected"));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThrows(IllegalStateException.class, () -> new RabbitOutboxTransport(rabbit).publish("nack", "BOOKING", "{}"));
    }
}
