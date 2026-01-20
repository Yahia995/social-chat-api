package com.socialchat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled cleanup tasks
 * ADDED: Centralized scheduled tasks to prevent memory leaks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledCleanupService {

    private final TokenRevocationService tokenRevocationService;
    private final WebSocketAuthorizationService webSocketAuthorizationService;

    /**
     * Clean up expired revoked tokens every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTokens() {
        try {
            tokenRevocationService.cleanupExpiredTokens();
            log.debug("Completed scheduled cleanup of expired tokens");
        } catch (Exception e) {
            log.error("Error during expired token cleanup", e);
        }
    }

    /**
     * Clean up expired rate limit entries every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupExpiredRateLimits() {
        try {
            webSocketAuthorizationService.cleanupExpiredEntries();
            log.debug("Completed scheduled cleanup of expired rate limits");
        } catch (Exception e) {
            log.error("Error during rate limit cleanup", e);
        }
    }
}