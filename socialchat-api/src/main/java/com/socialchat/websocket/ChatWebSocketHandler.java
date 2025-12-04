package com.socialchat.websocket;

import com.socialchat.dto.chat.MessageRequest;
import com.socialchat.dto.websocket.ChatMessageEvent;
import com.socialchat.dto.websocket.ReadReceiptEvent;
import com.socialchat.dto.websocket.TypingEvent;
import com.socialchat.security.JwtAuthenticationToken;
import com.socialchat.service.ChatService;
import com.socialchat.service.PresenceService;
import com.socialchat.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final PresenceService presenceService;
    private final ChatService chatService;
    private final WebSocketService webSocketService;

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user instanceof JwtAuthenticationToken jwtAuth) {
            presenceService.userConnected(jwtAuth.getUserId(), jwtAuth.getUsername());
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user instanceof JwtAuthenticationToken jwtAuth) {
            presenceService.userDisconnected(jwtAuth.getUserId(), jwtAuth.getUsername());
        }
    }

    @MessageMapping("/chat/{conversationId}/message")
    public void handleChatMessage(
            @DestinationVariable Long conversationId,
            @Payload ChatMessageEvent messageEvent,
            SimpMessageHeaderAccessor headerAccessor) {

        JwtAuthenticationToken auth = getJwtAuthentication(headerAccessor);
        if (auth == null) {
            log.warn("Received message without authentication");
            return;
        }

        Long userId = auth.getUserId();
        String username = auth.getUsername();

        log.debug("WebSocket message received from {} in conversation {}", username, conversationId);

        String content = messageEvent.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.warn("Empty message received from user {}", username);
            return;
        }

        if (content.length() > 5000) {
            log.warn("Message too long from user {}: {} chars", username, content.length());
            return;
        }

        // Persist message via ChatService (which also broadcasts via WebSocket)
        // Note: Authorization already checked in WebSocketSecurityConfig interceptor
        MessageRequest request = new MessageRequest();
        request.setContent(content.trim());

        try {
            chatService.sendMessageFromWebSocket(conversationId, userId, request);
        } catch (Exception e) {
            log.error("Error processing WebSocket message: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat/{conversationId}/typing")
    public void handleTyping(
            @DestinationVariable Long conversationId,
            @Payload TypingEvent event,
            SimpMessageHeaderAccessor headerAccessor) {

        JwtAuthenticationToken auth = getJwtAuthentication(headerAccessor);
        if (auth == null) return;

        // Note: Authorization already checked in WebSocketSecurityConfig interceptor
        webSocketService.sendTypingIndicator(
                conversationId,
                auth.getUserId(),
                auth.getUsername(),
                event.isTyping()
        );
    }

    @MessageMapping("/chat/{conversationId}/read")
    public void handleReadReceipt(
            @DestinationVariable Long conversationId,
            @Payload ReadReceiptEvent event,
            SimpMessageHeaderAccessor headerAccessor) {

        JwtAuthenticationToken auth = getJwtAuthentication(headerAccessor);
        if (auth == null) return;

        Long userId = auth.getUserId();
        if (userId != null) {
            // Note: Authorization already checked in WebSocketSecurityConfig interceptor
            try {
                chatService.markConversationAsReadFromWebSocket(conversationId, userId);
            } catch (Exception e) {
                log.error("Error marking conversation as read: {}", e.getMessage());
            }
        }
    }

    private JwtAuthenticationToken getJwtAuthentication(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth;
        }
        return null;
    }
}