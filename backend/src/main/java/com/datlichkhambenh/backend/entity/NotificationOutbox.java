package com.datlichkhambenh.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_outbox", indexes = @Index(name = "idx_outbox_dispatch", columnList = "status,next_attempt_at"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationOutbox {
    @Id @Column(length = 36) private String id;
    @Column(name = "event_key", nullable = false, unique = true, length = 160) private String eventKey;
    @Column(nullable = false, length = 20) private String kind;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String payload;
    @Column(nullable = false, length = 16) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private LocalDateTime nextAttemptAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "last_error_code", length = 80) private String lastErrorCode;
}
