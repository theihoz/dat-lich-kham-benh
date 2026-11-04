package com.datlichkhambenh.backend.messaging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Broker consumer failures are separate from exhausted outbox publisher retries. */
@Component
@Profile("!test & !demo")
public class NotificationQueueMetrics {
    public NotificationQueueMetrics(AmqpAdmin broker, MeterRegistry registry,
            @Value("${app.messaging.email-queue}") String queue) {
        String deadQueue=queue+".dlq";
        Gauge.builder("clinic.notifications.consumer.dead",broker,admin->{
            try {
                var properties=admin.getQueueProperties(deadQueue);
                if(properties==null) return Double.NaN;
                return ((Number)properties.get("QUEUE_MESSAGE_COUNT")).doubleValue();
            } catch(RuntimeException unavailable) {
                return Double.NaN; // Never claim zero backlog when the broker cannot be inspected.
            }
        }).tag("queue",deadQueue).description("Notifications exhausted consumer retries").register(registry);
    }
}
