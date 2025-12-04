package com.socialchat.controller;

import com.socialchat.dto.common.ApiResponse;
import com.socialchat.dto.common.PageResponse;
import com.socialchat.dto.friend.FriendRequestResponse;
import com.socialchat.dto.friend.FriendResponse;
import com.socialchat.dto.friend.RelationshipResponse;
import com.socialchat.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Friends", description = "Friend management endpoints")
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    @Operation(summary = "Get friends list")
    public ResponseEntity<ApiResponse<PageResponse<FriendResponse>>> getFriends(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getFriends(pageable)));
    }

    @PostMapping("/request/{userId}")
    @Operation(summary = "Send friend request")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendRequest(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        friendService.sendFriendRequest(userId),
                        "Friend request sent"
                ));
    }

    @GetMapping("/requests/received")
    @Operation(summary = "Get received friend requests")
    public ResponseEntity<ApiResponse<PageResponse<FriendRequestResponse>>> getReceivedRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getReceivedRequests(pageable)));
    }

    @GetMapping("/requests/sent")
    @Operation(summary = "Get sent friend requests")
    public ResponseEntity<ApiResponse<PageResponse<FriendRequestResponse>>> getSentRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getSentRequests(pageable)));
    }

    @PostMapping("/requests/{requestId}/accept")
    @Operation(summary = "Accept friend request")
    public ResponseEntity<ApiResponse<Void>> acceptRequest(@PathVariable Long requestId) {
        friendService.acceptFriendRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(null, "Friend request accepted"));
    }

    @PostMapping("/requests/{requestId}/reject")
    @Operation(summary = "Reject friend request")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(@PathVariable Long requestId) {
        friendService.rejectFriendRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(null, "Friend request rejected"));
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "Remove friend")
    public ResponseEntity<ApiResponse<Void>> removeFriend(@PathVariable Long friendId) {
        friendService.removeFriend(friendId);
        return ResponseEntity.ok(ApiResponse.success(null, "Friend removed"));
    }

    @PostMapping("/block/{userId}")
    @Operation(summary = "Block user")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long userId) {
        friendService.blockUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User blocked"));
    }

    @DeleteMapping("/block/{userId}")
    @Operation(summary = "Unblock user")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long userId) {
        friendService.unblockUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User unblocked"));
    }

    @GetMapping("/relationship/{userId}")
    @Operation(summary = "Get relationship status with user")
    public ResponseEntity<ApiResponse<RelationshipResponse>> getRelationship(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getRelationship(userId)));
    }
}
