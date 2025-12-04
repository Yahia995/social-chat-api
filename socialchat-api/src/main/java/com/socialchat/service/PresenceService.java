package com.socialchat.service;

import com.socialchat.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final WebSocketService webSocketService;
    private final FriendRequestRepository friendRequestRepository;

    // Map<userId, ConnectionInfo>
    private final Map<Long, ConnectionInfo> onlineUsers = new ConcurrentHashMap<>();

    public void userConnected(Long userId, String username) {
        if (userId == null || username == null) return;

        onlineUsers.put(userId, new ConnectionInfo(username, Instant.now()));
        log.info("User connected: {} (ID: {})", username, userId);

        broadcastPresenceToFriends(userId, username, true);
    }

    public void userDisconnected(Long userId, String username) {
        if (userId == null) return;

        ConnectionInfo info = onlineUsers.remove(userId);
        if (info != null) {
            log.info("User disconnected: {} (ID: {})", info.username(), userId);
            broadcastPresenceToFriends(userId, info.username(), false);
        }
    }

    private void broadcastPresenceToFriends(Long userId, String username, boolean online) {
        Set<Long> friendIds = friendRequestRepository.findFriendIdsByUserId(userId);

        for (Long friendId : friendIds) {
            ConnectionInfo friendInfo = onlineUsers.get(friendId);
            if (friendInfo != null) {
                // Friend is online, send them the presence update
                webSocketService.sendPresenceUpdateToUser(friendInfo.username(), userId, username, online);
            }
        }
        log.debug("Presence update sent to {} friends for user {}", friendIds.size(), username);
    }

    public boolean isUserOnline(Long userId) {
        return userId != null && onlineUsers.containsKey(userId);
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.values().stream()
                .anyMatch(info -> info.username().equals(username));
    }

    public Set<Long> getOnlineFriendIds(Long userId) {
        Set<Long> friendIds = friendRequestRepository.findFriendIdsByUserId(userId);
        return friendIds.stream()
                .filter(onlineUsers::containsKey)
                .collect(Collectors.toSet());
    }

    public boolean canSeePresence(Long viewerId, Long targetId) {
        return friendRequestRepository.areFriends(viewerId, targetId);
    }

    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    // Record to store connection info
    private record ConnectionInfo(String username, Instant connectedAt) {}
}