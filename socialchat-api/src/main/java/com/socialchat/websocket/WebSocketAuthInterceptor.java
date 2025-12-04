package com.socialchat.websocket;

import com.socialchat.security.JwtAuthenticationToken;
import com.socialchat.security.JwtClaims;
import com.socialchat.security.JwtService;
import com.socialchat.service.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    Optional<JwtClaims> claimsOpt = jwtService.validateAndParse(token);

                    if (claimsOpt.isPresent()) {
                        JwtClaims claims = claimsOpt.get();

                        // Check if token is revoked
                        if (tokenRevocationService.isTokenRevoked(claims.getTokenId())) {
                            log.debug("WebSocket connection rejected - token revoked");
                            return null;
                        }

                        // Build authorities from JWT roles
                        List<SimpleGrantedAuthority> authorities = claims.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();

                        // Create authentication from JWT claims (no DB call)
                        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                                claims.getUserId(),
                                claims.getUsername(),
                                authorities
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);

                        log.debug("WebSocket authenticated for user: {}", claims.getUsername());
                    }
                } catch (Exception e) {
                    log.error("WebSocket authentication failed", e);
                }
            }
        }

        return message;
    }
}
