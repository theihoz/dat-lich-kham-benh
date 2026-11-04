package com.datlichkhambenh.backend.messaging;

import com.datlichkhambenh.backend.entity.NotificationReceipt;
import com.datlichkhambenh.backend.pattern.notification.*;
import com.datlichkhambenh.backend.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

/** Receipt insert is flushed before SMTP and committed afterwards. SMTP remains at-least-once on crash. */
@Service
@RequiredArgsConstructor
public class DurableNotificationConsumer {
    private final NotificationReceiptRepository receipts;
    private final AppointmentRepository appointments;
    private final NotificationCreatorRegistry creators;
    private final NotificationSender sender;
    private final ObjectMapper json;
    private final MeterRegistry metrics;
    @Value("${app.clinic-zone:Asia/Ho_Chi_Minh}") private String clinicZone = "Asia/Ho_Chi_Minh";

    @Transactional
    public void deliver(String messageId, String kind, String payload) {
        if (messageId == null || messageId.isBlank() || messageId.length() > 80) {
            throw new IllegalArgumentException("Notification message ID required");
        }
        if (receipts.existsById(messageId)) {
            metrics.counter("clinic.notifications.consume", "result", "duplicate").increment(); return;
        }
        // Concurrent duplicates conflict on this primary key before either can send SMTP.
        // A failed insert rolls back and is retried by the container; it then sees the committed receipt.
        receipts.saveAndFlush(NotificationReceipt.builder().messageId(messageId)
                .deliveredAt(LocalDateTime.now(ZoneOffset.UTC)).build());
        try {
            if ("BOOKING".equals(kind)) {
                EmailNotificationMessage message = json.readValue(payload, EmailNotificationMessage.class);
                if ("APPOINTMENT_REMINDER".equals(message.type()) && !reminderStillDue(message.bookingCode())) {
                    metrics.counter("clinic.notifications.consume", "result", "obsolete").increment(); return;
                }
                sender.send(message.recipientEmail(), creators.get(message.type()).createMessage(message));
            } else if ("ACCOUNT".equals(kind)) {
                AccountNotificationMessage message = json.readValue(payload, AccountNotificationMessage.class);
                String subject = switch (message.type()) {
                    case "PASSWORD_RESET" -> "Đặt lại mật khẩu phòng khám";
                    case "EMAIL_VERIFICATION" -> "Xác minh email phòng khám";
                    default -> throw new IllegalArgumentException("Unsupported account notification type");
                };
                sender.send(message.recipientEmail(), new ConfirmationContent(subject,
                        "Mở liên kết sau để tiếp tục: " + message.link() + "\nNếu bạn không yêu cầu, hãy bỏ qua thư này."));
            } else { throw new IllegalArgumentException("Unsupported notification kind"); }
            metrics.counter("clinic.notifications.consume", "result", "delivered").increment();
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid notification payload");
        }
    }
    private boolean reminderStillDue(String bookingCode) {
        return appointments.findByBookingCodeForUpdate(bookingCode).filter(a -> "CONFIRMED".equals(a.getStatus()))
                .filter(a -> LocalDateTime.of(a.getSchedule().getWorkDate(), a.getSchedule().getStartTime())
                        .isAfter(LocalDateTime.now(ZoneId.of(clinicZone)))).isPresent();
    }
}
