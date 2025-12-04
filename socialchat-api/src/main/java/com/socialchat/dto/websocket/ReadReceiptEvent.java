package com.socialchat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptEvent {
    private Long conversationId;
    private Long userId;
    private String username;
    private LocalDateTime readAt;
}
