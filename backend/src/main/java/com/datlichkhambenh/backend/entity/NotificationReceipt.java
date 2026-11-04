package com.datlichkhambenh.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_receipts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationReceipt implements org.springframework.data.domain.Persistable<String> {
    @Id @Column(name = "message_id", length = 80) private String messageId;
    @Column(name = "delivered_at", nullable = false) private LocalDateTime deliveredAt;
    @Override public String getId() { return messageId; }
    /** Receipts are insert-only: never merge an existing ID and accidentally send it again. */
    @Override @Transient public boolean isNew() { return true; }
}
