package com.socialchat.service;

import com.socialchat.dto.websocket.PresenceEvent;
import com.socialchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final WebSocketService webSocketService;
    private final UserRepository userRepository;

    // Map<username, ConnectionInfo>
    private final Map<String, ConnectionInfo> onlineUsers = new ConcurrentHashMap<>();

    public void userConnected(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            onlineUsers.put(username, new ConnectionInfo(user.getId(), Instant.now()));
            log.info("User connected: {} (ID: {})", username, user.getId());

            // Broadcast presence with userId
            webSocketService.sendPresenceUpdate(user.getId(), username, true);
        });
    }

    public void userDisconnected(String username) {
        ConnectionInfo info = onlineUsers.remove(username);
        if (info != null) {
            log.info("User disconnected: {} (ID: {})", username, info.userId());
            webSocketService.sendPresenceUpdate(info.userId(), username, false);
        }
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public boolean isUserOnline(Long userId) {
        return onlineUsers.values().stream()
                .anyMatch(info -> info.userId().equals(userId));
    }

    public Set<String> getOnlineUsernames() {
        return onlineUsers.keySet();
    }

    public Set<Long> getOnlineUserIds() {
        return onlineUsers.values().stream()
                .map(ConnectionInfo::userId)
                .collect(java.util.stream.Collectors.toSet());
    }

    // Record to store connection info
    private record ConnectionInfo(Long userId, Instant connectedAt) {}
}
