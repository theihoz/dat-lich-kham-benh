package com.datlichkhambenh.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_audit_entries", indexes = @Index(name = "idx_audit_record_time", columnList = "medical_record_id,occurred_at"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicalAuditEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "actor_user_id", nullable = false) private Long actorUserId;
    @Column(nullable = false, length = 32) private String action;
    @Column(name = "medical_record_id") private Long medicalRecordId;
    @Column(name = "appointment_id") private Long appointmentId;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
}
