package com.socialchat.dto.chat;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRequest {

    @NotEmpty(message = "Participant IDs are required")
    private List<Long> participantIds;

    private String name; // Optional, for group chats
}
