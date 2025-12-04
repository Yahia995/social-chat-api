package com.socialchat.controller;

import com.socialchat.dto.common.ApiResponse;
import com.socialchat.dto.common.PageResponse;
import com.socialchat.dto.post.*;
import com.socialchat.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Posts", description = "Post management endpoints")
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new post")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestPart("post") @Valid PostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        postService.createPost(request, images),
                        "Post created successfully"
                ));
    }

    @GetMapping("/feed")
    @Operation(summary = "Get posts feed")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getFeed(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(postService.getFeed(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(id)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getUserPosts(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(postService.getUserPosts(userId, pageable)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update post")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.updatePost(id, request),
                "Post updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete post")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Post deleted successfully"));
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "Like a post")
    public ResponseEntity<ApiResponse<LikeResponse>> likePost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.likePost(id),
                "Post liked"
        ));
    }

    @DeleteMapping("/{id}/like")
    @Operation(summary = "Unlike a post")
    public ResponseEntity<ApiResponse<LikeResponse>> unlikePost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.unlikePost(id),
                "Post unliked"
        ));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add comment to post")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        postService.addComment(id, request),
                        "Comment added"
                ));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "Get comments for post")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(postService.getComments(id, pageable)));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @Operation(summary = "Delete comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        postService.deleteComment(postId, commentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted"));
    }
}
