package com.socialchat.controller;

import com.socialchat.dto.chat.*;
import com.socialchat.dto.common.ApiResponse;
import com.socialchat.dto.common.PageResponse;
import com.socialchat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chat", description = "Chat and messaging endpoints")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations")
    @Operation(summary = "Create or get conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody ConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(chatService.createOrGetConversation(request)));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Get all conversations")
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> getConversations(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getConversations(pageable)));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get conversation by ID")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getConversation(id)));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Get messages in conversation")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @PathVariable Long id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessages(id, pageable)));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "Send message")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody MessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        chatService.sendMessage(id, request),
                        "Message sent"
                ));
    }

    @PostMapping("/conversations/{id}/read")
    @Operation(summary = "Mark conversation as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        chatService.markConversationAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Leave conversation")
    public ResponseEntity<ApiResponse<Void>> leaveConversation(@PathVariable Long id) {
        chatService.leaveConversation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Left conversation"));
    }
}
