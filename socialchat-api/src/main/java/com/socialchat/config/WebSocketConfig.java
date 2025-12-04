package com.socialchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final Environment environment;

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    public WebSocketConfig(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("ssl");

        String[] origins = isProd ? allowedOrigins.split(",") : new String[]{"*"};

        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS();

        // Native WebSocket endpoint (WSS in prod, WS in dev)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);
    }
}