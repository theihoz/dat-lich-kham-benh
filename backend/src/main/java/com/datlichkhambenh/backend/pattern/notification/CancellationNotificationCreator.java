package com.datlichkhambenh.backend.pattern.notification;

import com.datlichkhambenh.backend.messaging.EmailNotificationMessage;
import org.springframework.stereotype.Component;

@Component
public class CancellationNotificationCreator extends NotificationCreator {
    @Override public String type() { return "BOOKING_CANCELLED"; }
    @Override public NotificationContent createMessage(EmailNotificationMessage n) {
        return new CancellationContent("Thông báo hủy lịch khám - Mã: " + n.bookingCode(),
                "Kính gửi %s,\n\nLịch khám mã %s đã được hủy thành công."
                        .formatted(n.patientName(), n.bookingCode()));
    }
}
