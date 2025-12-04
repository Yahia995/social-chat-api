package com.socialchat.service;

import com.socialchat.dto.chat.MessageResponse;
import com.socialchat.dto.websocket.NotificationEvent;
import com.socialchat.dto.websocket.PresenceEvent;
import com.socialchat.dto.websocket.ReadReceiptEvent;
import com.socialchat.dto.websocket.TypingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // ==================== CHAT MESSAGES ====================

    public void sendChatMessage(Long conversationId, MessageResponse message) {
        String destination = "/topic/conversations/" + conversationId + "/messages";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Message sent to conversation {}", conversationId);
    }

    // ==================== READ RECEIPTS ====================

    public void sendReadReceipt(Long conversationId, Long userId, LocalDateTime readAt) {
        ReadReceiptEvent event = ReadReceiptEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .readAt(readAt)
                .build();

        String destination = "/topic/conversations/" + conversationId + "/read-receipts";
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Read receipt sent for conversation {} by user {}", conversationId, userId);
    }

    // ==================== TYPING INDICATORS ====================

    public void sendTypingIndicator(Long conversationId, Long userId, String username, boolean isTyping) {
        TypingEvent event = TypingEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .username(username)
                .typing(isTyping)
                .build();

        String destination = "/topic/conversations/" + conversationId + "/typing";
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Typing indicator sent for conversation {} by user {}: {}", conversationId, userId, isTyping);
    }

    // ==================== PRESENCE ====================

    @Deprecated
    public void sendPresenceUpdate(Long userId, String username, boolean online) {
        PresenceEvent event = PresenceEvent.builder()
                .userId(userId)
                .username(username)
                .online(online)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/presence", event);
        log.debug("Presence update broadcast for user {}: {}", username, online ? "online" : "offline");
    }

    public void sendPresenceUpdateToUser(String targetUsername, Long userId, String username, boolean online) {
        PresenceEvent event = PresenceEvent.builder()
                .userId(userId)
                .username(username)
                .online(online)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(targetUsername, "/queue/presence", event);
        log.debug("Presence update sent to {} for user {}: {}", targetUsername, username, online ? "online" : "offline");
    }

    // ==================== NOTIFICATIONS ====================

    public void sendNotificationToUser(String username, NotificationEvent notification) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
        log.debug("Notification sent to user {}: {}", username, notification.getType());
    }

    // ==================== GENERIC USER MESSAGING ====================

    public void sendToUser(String username, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(username, destination, payload);
        log.debug("Message sent to user {} at {}", username, destination);
    }

    public void sendToTopic(String topic, Object payload) {
        messagingTemplate.convertAndSend("/topic/" + topic, payload);
        log.debug("Message sent to topic: {}", topic);
    }
}
