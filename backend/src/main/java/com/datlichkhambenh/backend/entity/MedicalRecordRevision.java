package com.datlichkhambenh.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_record_revisions", indexes = @Index(name = "idx_revisions_record", columnList = "medical_record_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicalRecordRevision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "medical_record_id", nullable = false) private Long medicalRecordId;
    @Column(name = "actor_user_id", nullable = false) private Long actorUserId;
    @Column(name = "before_diagnosis", columnDefinition = "LONGTEXT") private String beforeDiagnosis;
    @Column(name = "before_notes", columnDefinition = "LONGTEXT") private String beforeNotes;
    @Column(name = "after_diagnosis", columnDefinition = "LONGTEXT") private String afterDiagnosis;
    @Column(name = "after_notes", columnDefinition = "LONGTEXT") private String afterNotes;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
}
