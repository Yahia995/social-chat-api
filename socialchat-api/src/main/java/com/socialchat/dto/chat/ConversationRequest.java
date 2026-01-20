package com.socialchat.dto.chat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRequest {

    // FIXED: Added @NotNull and proper initialization
    @NotNull(message = "Participant IDs cannot be null")
    @NotEmpty(message = "Participant IDs are required")
    @Builder.Default
    private List<Long> participantIds = new ArrayList<>();

    @Size(max = 100, message = "Conversation name must not exceed 100 characters")
    private String name; // Optional, for group chats
}