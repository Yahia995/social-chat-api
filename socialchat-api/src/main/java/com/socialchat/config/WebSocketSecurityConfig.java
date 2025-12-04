package com.socialchat.config;

import com.socialchat.security.JwtAuthenticationToken;
import com.socialchat.security.JwtClaims;
import com.socialchat.security.JwtService;
import com.socialchat.service.TokenRevocationService;
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

@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return message;
                }

                String authHeader = accessor.getFirstNativeHeader("Authorization");

                if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                    log.warn("WebSocket CONNECT without Authorization header");
                    throw new IllegalArgumentException("Authorization header required");
                }

                String token = authHeader.substring(BEARER_PREFIX.length());

                JwtClaims claims = jwtService.validateAndParse(token)
                        .filter(c -> c.isAccessToken())
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
        });
    }
}
