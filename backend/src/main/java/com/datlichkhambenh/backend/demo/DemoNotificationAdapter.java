package com.datlichkhambenh.backend.demo;

import com.datlichkhambenh.backend.pattern.notification.NotificationContent;
import com.datlichkhambenh.backend.pattern.notification.NotificationSender;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;

/** Hộp thư cục bộ có giới hạn; không gửi email ra ngoài trong profile demo. */
@Component
@Profile("demo")
public class DemoNotificationAdapter implements NotificationSender {
    private final ArrayDeque<Entry> inbox = new ArrayDeque<>();
    public record Entry(LocalDateTime time, String recipient, String subject, String body) { }

    @Override public synchronized void send(String recipient, NotificationContent content) {
        if (inbox.size() == 100) inbox.removeLast();
        inbox.addFirst(new Entry(LocalDateTime.now(), recipient, content.subject(), content.body()));
    }

    public synchronized List<Entry> entries() { return List.copyOf(inbox); }
}
