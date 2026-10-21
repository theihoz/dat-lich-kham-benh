package com.datlichkhambenh.backend.messaging;

import com.datlichkhambenh.backend.pattern.notification.NotificationContent;
import com.datlichkhambenh.backend.pattern.notification.NotificationCreatorRegistry;
import com.datlichkhambenh.backend.pattern.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailQueueConsumer {
    private final NotificationCreatorRegistry creators;
    private final NotificationSender sender;

    @RabbitListener(queues = "${app.messaging.email-queue}")
    public void consume(EmailNotificationMessage notification) {
        NotificationContent content = creators.get(notification.type()).createMessage(notification);
        sender.send(notification.recipientEmail(), content);
        log.info("Delivered {} notification for booking {}", notification.type(), notification.bookingCode());
    }
}
