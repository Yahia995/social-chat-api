package com.socialchat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to store revoked access tokens.
 * When a user logs out, their access token is added here.
 * Any token in this table is considered invalid.
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String tokenId;  // JWT ID (jti claim)

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;  // When the token would naturally expire

    @CreationTimestamp
    @Column(name = "revoked_at", updatable = false)
    private LocalDateTime revokedAt;
}
