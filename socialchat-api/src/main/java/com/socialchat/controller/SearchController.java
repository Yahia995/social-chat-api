package com.socialchat.controller;

import com.socialchat.dto.common.ApiResponse;
import com.socialchat.dto.common.PageResponse;
import com.socialchat.dto.user.UserResponse;
import com.socialchat.mapper.UserMapper;
import com.socialchat.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Search", description = "Search endpoints")
public class SearchController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping("/users")
    @Operation(summary = "Search users by username or display name")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        var page = userRepository.searchByUsernameOrDisplayName(q, pageable);

        var response = PageResponse.<UserResponse>builder()
                .content(page.getContent().stream()
                        .map(userMapper::toResponse)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
