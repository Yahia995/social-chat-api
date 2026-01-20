package com.socialchat.config;

import com.socialchat.security.JwtAuthenticationToken;
import com.socialchat.security.JwtClaims;
import com.socialchat.security.JwtService;
import com.socialchat.service.TokenRevocationService;
import com.socialchat.service.WebSocketAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private static final String BEARER_PREFIX = "Bearer ";

    // FIXED: Corrected pattern to match actual subscription paths
    private static final Pattern CONVERSATION_TOPIC_PATTERN = Pattern.compile("/topic/conversations/(\\d+)(?:/messages|/typing|/read-receipts)?");
    private static final Pattern CONVERSATION_APP_PATTERN = Pattern.compile("/app/chat/(\\d+)(?:/message|/typing|/read)?");
    private static final Pattern USER_QUEUE_PATTERN = Pattern.compile("/user/queue/.*");

    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;
    private final WebSocketAuthorizationService authorizationService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();

                if (StompCommand.CONNECT.equals(command)) {
                    return handleConnect(message, accessor);
                }

                if (StompCommand.SUBSCRIBE.equals(command)) {
                    return handleSubscribe(message, accessor);
                }

                if (StompCommand.SEND.equals(command)) {
                    return handleSend(message, accessor);
                }

                return message;
            }
        });
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("WebSocket CONNECT without Authorization header");
            throw new IllegalArgumentException("Authorization header required");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        JwtClaims claims = jwtService.validateAndParse(token)
                .filter(JwtClaims::isAccessToken)
                .filter(c -> !tokenRevocationService.isTokenRevoked(c.getTokenId()))
                .orElseThrow(() -> {
                    log.warn("Invalid or revoked JWT in WebSocket CONNECT");
                    return new IllegalArgumentException("Invalid or revoked token");
                });

        List<SimpleGrantedAuthority> authorities = claims.getRoles() != null
                ? claims.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList()
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                claims.getUserId(),
                claims.getUsername(),
                authorities
        );

        accessor.setUser(auth);
        log.debug("WebSocket authenticated: {} (id: {})", claims.getUsername(), claims.getUserId());

        return message;
    }

    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        JwtAuthenticationToken auth = getAuthentication(accessor);

        if (destination == null || auth == null) {
            log.warn("SUBSCRIBE without destination or authentication");
            throw new IllegalArgumentException("Invalid subscription request");
        }

        log.debug("SUBSCRIBE request from {} to {}", auth.getUsername(), destination);

        // Allow user-specific queues (notifications, presence, etc.)
        if (USER_QUEUE_PATTERN.matcher(destination).matches()) {
            return message;
        }

        // FIXED: Block public presence topic subscription
        if (destination.equals("/topic/presence")) {
            log.warn("User {} attempted to subscribe to public presence topic - denied", auth.getUsername());
            throw new IllegalArgumentException("Public presence topic not allowed. Subscribe to /user/queue/presence for friends-only presence.");
        }

        // Allow public notifications topic (deprecated, but keeping for backwards compatibility)
        if (destination.startsWith("/topic/notifications")) {
            log.warn("User {} subscribed to deprecated public notifications topic", auth.getUsername());
            return message;
        }

        // Validate conversation membership for conversation-specific topics
        Matcher matcher = CONVERSATION_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            Long conversationId = Long.parseLong(matcher.group(1));

            if (!authorizationService.isUserInConversation(auth.getUserId(), conversationId)) {
                log.warn("User {} attempted to subscribe to conversation {} without membership",
                        auth.getUsername(), conversationId);
                throw new IllegalArgumentException("Not a member of this conversation");
            }

            log.debug("User {} authorized to subscribe to conversation {}", auth.getUsername(), conversationId);
            return message;
        }

        // Reject unknown destinations
        log.warn("SUBSCRIBE to unknown destination: {} by user {}", destination, auth.getUsername());
        throw new IllegalArgumentException("Invalid subscription destination");
    }

    private Message<?> handleSend(Message<?> message, StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        JwtAuthenticationToken auth = getAuthentication(accessor);

        if (destination == null || auth == null) {
            log.warn("SEND without destination or authentication");
            throw new IllegalArgumentException("Invalid send request");
        }

        log.debug("SEND request from {} to {}", auth.getUsername(), destination);

        Matcher matcher = CONVERSATION_APP_PATTERN.matcher(destination);
        if (matcher.matches()) {
            Long conversationId = Long.parseLong(matcher.group(1));

            if (!authorizationService.isUserInConversation(auth.getUserId(), conversationId)) {
                log.warn("User {} attempted to send to conversation {} without membership",
                        auth.getUsername(), conversationId);
                throw new IllegalArgumentException("Not a member of this conversation");
            }

            if (!authorizationService.checkRateLimit(auth.getUserId(), conversationId)) {
                log.warn("Rate limit exceeded for user {} in conversation {}",
                        auth.getUsername(), conversationId);
                throw new IllegalArgumentException("Rate limit exceeded. Please slow down.");
            }

            log.debug("User {} authorized to send to conversation {}", auth.getUsername(), conversationId);
            return message;
        }

        // Reject unknown destinations
        log.warn("SEND to unknown destination: {} by user {}", destination, auth.getUsername());
        throw new IllegalArgumentException("Invalid send destination");
    }

    private JwtAuthenticationToken getAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth;
        }
        return null;
    }
}