package com.datlichkhambenh.backend.pattern.notification;

import com.datlichkhambenh.backend.messaging.EmailNotificationMessage;
import org.springframework.stereotype.Component;

@Component
public class ReminderNotificationCreator extends NotificationCreator {
    @Override public String type() { return "APPOINTMENT_REMINDER"; }
    @Override public NotificationContent createMessage(EmailNotificationMessage n) {
        return new ReminderContent("Nhắc lịch khám - Mã: " + n.bookingCode(),
                appointmentDetails(n, "Đây là thông báo nhắc lịch khám của bạn."));
    }
}
