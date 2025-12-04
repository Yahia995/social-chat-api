package com.socialchat.controller;

import com.socialchat.dto.websocket.PresenceEvent;
import com.socialchat.service.PresenceService;
import com.socialchat.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
@Tag(name = "Presence", description = "User online/offline status APIs")
public class PresenceController {

    private final PresenceService presenceService;
    private final SecurityUtils securityUtils;

    @GetMapping("/friends")
    @Operation(summary = "Get online friends", description = "Returns IDs of friends currently online")
    public ResponseEntity<Set<Long>> getOnlineFriends() {
        Long currentUserId = securityUtils.getCurrentUserId();
        Set<Long> onlineFriendIds = presenceService.getOnlineFriendIds(currentUserId);
        return ResponseEntity.ok(onlineFriendIds);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Check user online status", description = "Check if a specific user is online (must be friends)")
    public ResponseEntity<PresenceEvent> getUserPresence(@PathVariable Long userId) {
        Long currentUserId = securityUtils.getCurrentUserId();

        // Only allow checking presence of friends
        if (!presenceService.canSeePresence(currentUserId, userId)) {
            return ResponseEntity.notFound().build();
        }

        boolean isOnline = presenceService.isUserOnline(userId);
        PresenceEvent event = PresenceEvent.builder()
                .userId(userId)
                .online(isOnline)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(event);
    }
}