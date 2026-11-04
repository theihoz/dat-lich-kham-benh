package com.datlichkhambenh.backend.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="auth_refresh_sessions", indexes={@Index(name="ix_auth_session_family",columnList="family_id"), @Index(name="ix_auth_session_user",columnList="user_id")})
@Getter @Setter @NoArgsConstructor
public class AuthRefreshSession {
 @Id @Column(length=36) private String id;
 @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
 @Column(name="family_id",nullable=false,length=36) private String familyId;
 @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(name="credentials_version",nullable=false) private long credentialsVersion;
 @Column(name="expires_at",nullable=false) private Instant expiresAt;
 @Column(name="revoked_at") private Instant revokedAt;
}
