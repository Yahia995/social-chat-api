package com.socialchat.service;

import com.socialchat.entity.RevokedToken;
import com.socialchat.repository.RevokedTokenRepository;
import com.socialchat.security.JwtClaims;
import com.socialchat.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    /**
     * Check if a token ID has been revoked.
     * Use this when you already have parsed claims.
     */
    public boolean isTokenRevoked(String tokenId) {
        if (tokenId == null) {
            return true;
        }
        return revokedTokenRepository.existsByTokenId(tokenId);
    }

    /**
     * Check if a raw token has been revoked.
     * Parses the token to extract tokenId.
     */
    public boolean isTokenRevokedByRawToken(String token) {
        return jwtService.parseToken(token)
                .map(claims -> isTokenRevoked(claims.getTokenId()))
                .orElse(true);
    }

    /**
     * Revoke a token using pre-parsed claims.
     * More efficient when claims are already available.
     */
    @Transactional
    public void revokeToken(JwtClaims claims, Long userId) {
        if (claims.getTokenId() == null) {
            log.warn("Cannot revoke token without jti claim");
            return;
        }

        if (revokedTokenRepository.existsByTokenId(claims.getTokenId())) {
            return; // Already revoked
        }

        RevokedToken revokedToken = RevokedToken.builder()
                .tokenId(claims.getTokenId())
                .userId(userId)
                .expiresAt(LocalDateTime.ofInstant(
                        claims.getExpiration().toInstant(),
                        ZoneId.systemDefault()))
                .build();

        revokedTokenRepository.save(revokedToken);
        log.debug("Token revoked for user: {}", userId);
    }

    @Transactional
    public void revokeToken(String token, Long userId) {
        jwtService.parseToken(token).ifPresent(claims -> revokeToken(claims, userId));
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        revokedTokenRepository.deleteExpiredTokens();
        log.debug("Cleaned up expired revoked tokens");
    }
}
