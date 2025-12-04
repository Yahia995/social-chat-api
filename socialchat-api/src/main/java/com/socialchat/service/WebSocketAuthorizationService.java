package com.socialchat.service;

import com.socialchat.repository.ConversationParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for WebSocket authorization checks.
 * Validates conversation membership and enforces rate limiting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketAuthorizationService {

    private final ConversationParticipantRepository participantRepository;

    // Rate limiting: max messages per user per conversation per time window
    private static final int MAX_MESSAGES_PER_WINDOW = 30;
    private static final long WINDOW_DURATION_SECONDS = 60;

    // Cache for rate limiting: key = "userId:conversationId"
    private final Map<String, RateLimitEntry> rateLimitCache = new ConcurrentHashMap<>();

    /**
     * Check if user is a member of the conversation.
     * Uses database query - could be cached for performance.
     */
    public boolean isUserInConversation(Long userId, Long conversationId) {
        if (userId == null || conversationId == null) {
            return false;
        }

        try {
            return participantRepository.existsByConversationIdAndUserId(conversationId, userId);
        } catch (Exception e) {
            log.error("Error checking conversation membership: userId={}, conversationId={}",
                    userId, conversationId, e);
            return false;
        }
    }

    /**
     * Check and update rate limit for a user in a conversation.
     * Returns true if within limit, false if exceeded.
     */
    public boolean checkRateLimit(Long userId, Long conversationId) {
        if (userId == null || conversationId == null) {
            return false;
        }

        String key = userId + ":" + conversationId;
        Instant now = Instant.now();

        RateLimitEntry entry = rateLimitCache.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now, WINDOW_DURATION_SECONDS)) {
                // Start new window
                return new RateLimitEntry(now, new AtomicInteger(1));
            }
            // Increment counter in existing window
            existing.getCount().incrementAndGet();
            return existing;
        });

        boolean allowed = entry.getCount().get() <= MAX_MESSAGES_PER_WINDOW;

        if (!allowed) {
            log.debug("Rate limit reached for user {} in conversation {}: {} messages in window",
                    userId, conversationId, entry.getCount().get());
        }

        return allowed;
    }

    /**
     * Clear rate limit cache - for testing or admin purposes.
     */
    public void clearRateLimitCache() {
        rateLimitCache.clear();
    }

    /**
     * Get current message count for a user in a conversation.
     */
    public int getCurrentMessageCount(Long userId, Long conversationId) {
        String key = userId + ":" + conversationId;
        RateLimitEntry entry = rateLimitCache.get(key);
        return entry != null ? entry.getCount().get() : 0;
    }

    private static class RateLimitEntry {
        private final Instant windowStart;
        private final AtomicInteger count;

        RateLimitEntry(Instant windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }

        boolean isExpired(Instant now, long windowDurationSeconds) {
            return windowStart.plusSeconds(windowDurationSeconds).isBefore(now);
        }

        AtomicInteger getCount() {
            return count;
        }
    }
}