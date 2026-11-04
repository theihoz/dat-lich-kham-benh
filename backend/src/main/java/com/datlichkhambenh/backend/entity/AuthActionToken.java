package com.datlichkhambenh.backend.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="auth_action_tokens",indexes=@Index(name="ix_auth_action_user",columnList="user_id,purpose"))
@Getter @Setter @NoArgsConstructor
public class AuthActionToken {
 @Id @Column(name="token_hash",length=64) private String tokenHash;
 @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false,length=32) private String purpose;
 @Column(name="expires_at",nullable=false) private Instant expiresAt;
 @Column(name="consumed_at") private Instant consumedAt;
}
