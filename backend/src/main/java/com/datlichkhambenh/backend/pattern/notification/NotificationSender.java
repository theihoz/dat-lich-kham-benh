package com.datlichkhambenh.backend.pattern.notification;

/** Target của Adapter: nghiệp vụ không phụ thuộc API JavaMail. */
public interface NotificationSender {
    void send(String recipient, NotificationContent content);
}
