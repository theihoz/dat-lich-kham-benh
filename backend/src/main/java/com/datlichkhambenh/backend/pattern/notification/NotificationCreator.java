package com.datlichkhambenh.backend.pattern.notification;

import com.datlichkhambenh.backend.messaging.EmailNotificationMessage;

/** Factory Method: lớp con quyết định nội dung thông báo được tạo. */
public abstract class NotificationCreator {
    public abstract String type();
    public abstract NotificationContent createMessage(EmailNotificationMessage notification);

    protected String appointmentDetails(EmailNotificationMessage n, String introduction) {
        return ("Kính gửi %s,\n\n%s\nMã đặt chỗ: %s\nBác sĩ: %s\nNgày khám: %s\n"
                + "Thời gian: %s - %s\nPhí khám: %s VNĐ")
                .formatted(n.patientName(), introduction, n.bookingCode(), n.doctorName(),
                        n.workDate(), n.startTime(), n.endTime(), n.price());
    }
}
