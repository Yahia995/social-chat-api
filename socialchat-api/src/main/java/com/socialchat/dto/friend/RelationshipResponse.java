package com.socialchat.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipResponse {
    private String status; // none, request_sent, request_received, friends
    private Long requestId; // if there's a pending request
}
