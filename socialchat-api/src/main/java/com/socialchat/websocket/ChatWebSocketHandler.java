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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("app/")
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final PresenceService presenceService;
    private final ChatService chatService;
    private final WebSocketService webSocketService;

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user != null) {
            presenceService.userConnected(user.getName());
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user != null) {
            presenceService.userDisconnected(user.getName());
        }
    }

    @MessageMapping("/chat/{conversationId}/message")
    public void handleChatMessage(
            @DestinationVariable Long conversationId,
            @Payload ChatMessageEvent messageEvent,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        // Extract userId from JwtAuthenticationToken
        Long userId = null;
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            userId = jwtToken.getUserId();
        }

        log.debug("WebSocket message received from {} in conversation {}", principal.getName(), conversationId);

        // Persist message via ChatService (which also broadcasts via WebSocket)
        MessageRequest request = new MessageRequest();
        request.setContent(messageEvent.getContent());

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

        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        // Extract userId from JwtAuthenticationToken
        Long userId = null;
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            userId = jwtToken.getUserId();
        }

        // Use WebSocketService to broadcast typing indicator
        webSocketService.sendTypingIndicator(
                conversationId,
                userId,
                principal.getName(),
                event.isTyping()
        );
    }

    @MessageMapping("/chat/{conversationId}/read")
    public void handleReadReceipt(
            @DestinationVariable Long conversationId,
            @Payload ReadReceiptEvent event,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        // Extract userId from JwtAuthenticationToken
        Long userId = null;
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            userId = jwtToken.getUserId();
        }

        if (userId != null) {
            // Mark as read in database and broadcast
            try {
                chatService.markConversationAsReadFromWebSocket(conversationId, userId);
            } catch (Exception e) {
                log.error("Error marking conversation as read: {}", e.getMessage());
            }
        }
    }
}
