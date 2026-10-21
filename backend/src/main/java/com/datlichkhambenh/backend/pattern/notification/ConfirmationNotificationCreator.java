package com.datlichkhambenh.backend.pattern.notification;

import com.datlichkhambenh.backend.messaging.EmailNotificationMessage;
import org.springframework.stereotype.Component;

@Component
public class ConfirmationNotificationCreator extends NotificationCreator {
    @Override public String type() { return "BOOKING_CONFIRMED"; }
    @Override public NotificationContent createMessage(EmailNotificationMessage n) {
        return new ConfirmationContent("Xác nhận đặt lịch khám - Mã: " + n.bookingCode(),
                appointmentDetails(n, "Bạn đã đặt lịch khám thành công."));
    }
}
