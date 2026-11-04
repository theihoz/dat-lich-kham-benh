package com.datlichkhambenh.backend.messaging;

import com.datlichkhambenh.backend.messaging.outbox.OutboxDispatcher;
import com.datlichkhambenh.backend.repository.NotificationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import java.time.*;

@Configuration
@EnableScheduling
@Profile("!test")
@ConditionalOnProperty(name = "app.delivery.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class DeliverySchedulingConfig {
    private final OutboxDispatcher dispatcher;
    private final AppointmentReminderScheduler reminders;
    private final String clinicZone;
    public DeliverySchedulingConfig(OutboxDispatcher dispatcher, AppointmentReminderScheduler reminders,
            NotificationOutboxRepository notifications, MeterRegistry metrics,
            @Value("${app.clinic-zone:Asia/Ho_Chi_Minh}") String clinicZone) {
        this.dispatcher = dispatcher; this.reminders = reminders; this.clinicZone = clinicZone;
        metrics.gauge("clinic.notifications.outbox.pending", notifications, n -> n.countByStatus("PENDING"));
        metrics.gauge("clinic.notifications.outbox.dead", notifications, n -> n.countByStatus("DEAD"));
    }
    @Scheduled(fixedDelayString = "${app.delivery.poll-delay-ms:5000}", initialDelayString = "${app.delivery.initial-delay-ms:15000}")
    public void deliverPending() { dispatcher.dispatchBatch(); }
    @Scheduled(fixedDelayString = "${app.delivery.reminder-delay-ms:60000}", initialDelayString = "${app.delivery.initial-delay-ms:15000}")
    public void remindPatients() { reminders.enqueueDueReminders(LocalDateTime.now(ZoneId.of(clinicZone))); }
}
