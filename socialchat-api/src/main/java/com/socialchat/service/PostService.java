package com.socialchat.service;

import com.socialchat.dto.common.PageResponse;
import com.socialchat.dto.post.*;
import com.socialchat.entity.Comment;
import com.socialchat.entity.Post;
import com.socialchat.entity.PostLike;
import com.socialchat.entity.User;
import com.socialchat.exception.ForbiddenException;
import com.socialchat.exception.ResourceNotFoundException;
import com.socialchat.mapper.PostMapper;
import com.socialchat.repository.CommentRepository;
import com.socialchat.repository.PostLikeRepository;
import com.socialchat.repository.PostRepository;
import com.socialchat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final PostMapper postMapper;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Transactional
    public PostResponse createPost(PostRequest request, List<MultipartFile> images) {
        User currentUser = securityUtils.getCurrentUser();

        Post post = Post.builder()
                .user(currentUser)
                .content(request.getContent())
                .build();

        // Handle multiple images
        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String filename = fileStorageService.storeFile(image, "posts");
                    imageUrls.add("/media/" + filename);
                }
            }
            if (!imageUrls.isEmpty()) {
                // Store first image as main image URL, others could be stored in a separate field
                post.setImageUrl(imageUrls.get(0));
            }
        }

        post = postRepository.save(post);
        log.info("Post created by user: {}", currentUser.getUsername());

        return postMapper.toResponse(post, 0, 0, false);
    }

    public PageResponse<PostResponse> getFeed(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();

        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(post -> mapPostWithStats(post, currentUser))
                .collect(Collectors.toList());

        return PageResponse.<PostResponse>builder()
                .content(content)
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .first(posts.isFirst())
                .last(posts.isLast())
                .build();
    }

    public PostResponse getPost(Long postId) {
        User currentUser = securityUtils.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        return mapPostWithStats(post, currentUser);
    }

    public PageResponse<PostResponse> getUserPosts(Long userId, Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<PostResponse> content = posts.getContent().stream()
                .map(post -> mapPostWithStats(post, currentUser))
                .collect(Collectors.toList());

        return PageResponse.<PostResponse>builder()
                .content(content)
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .first(posts.isFirst())
                .last(posts.isLast())
                .build();
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot update this post");
        }

        post.setContent(request.getContent());
        post = postRepository.save(post);

        log.info("Post {} updated by user {}", postId, currentUser.getUsername());

        return mapPostWithStats(post, currentUser);
    }

    @Transactional
    public void deletePost(Long postId) {
        User currentUser = securityUtils.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot delete this post");
        }

        postRepository.delete(post);
        log.info("Post {} deleted by user {}", postId, currentUser.getUsername());
    }

    @Transactional
    public LikeResponse likePost(Long postId) {
        User currentUser = securityUtils.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (postLikeRepository.existsByPostAndUser(post, currentUser)) {
            // Already liked
            int count = postLikeRepository.countByPost(post);
            return LikeResponse.builder()
                    .liked(true)
                    .likeCount(count)
                    .build();
        }

        PostLike like = PostLike.builder()
                .post(post)
                .user(currentUser)
                .build();
        postLikeRepository.save(like);

        int count = postLikeRepository.countByPost(post);
        log.info("Post {} liked by user {}", postId, currentUser.getUsername());

        if (!post.getUser().getId().equals(currentUser.getId())) {
            String data = String.format("{\"postId\": %d, \"userId\": %d}", postId, currentUser.getId());
            notificationService.createNotification(
                    post.getUser(),
                    "POST_LIKE",
                    "New Like",
                    currentUser.getDisplayName() + " liked your post",
                    data
            );
        }

        return LikeResponse.builder()
                .liked(true)
                .likeCount(count)
                .build();
    }

    @Transactional
    public LikeResponse unlikePost(Long postId) {
        User currentUser = securityUtils.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        postLikeRepository.findByPostAndUser(post, currentUser)
                .ifPresent(postLikeRepository::delete);

        int count = postLikeRepository.countByPost(post);
        log.info("Post {} unliked by user {}", postId, currentUser.getUsername());

        return LikeResponse.builder()
                .liked(false)
                .likeCount(count)
                .build();
    }

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        Comment comment = Comment.builder()
                .post(post)
                .user(currentUser)
                .content(request.getContent())
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment added to post {} by user {}", postId, currentUser.getUsername());

        if (!post.getUser().getId().equals(currentUser.getId())) {
            String data = String.format("{\"postId\": %d, \"commentId\": %d, \"userId\": %d}",
                    postId, comment.getId(), currentUser.getId());
            notificationService.createNotification(
                    post.getUser(),
                    "POST_COMMENT",
                    "New Comment",
                    currentUser.getDisplayName() + " commented on your post",
                    data
            );
        }

        return postMapper.toCommentResponse(comment);
    }

    public PageResponse<CommentResponse> getComments(Long postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        Page<Comment> comments = commentRepository.findByPostOrderByCreatedAtDesc(post, pageable);

        List<CommentResponse> content = comments.getContent().stream()
                .map(postMapper::toCommentResponse)
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(content)
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .first(comments.isFirst())
                .last(comments.isLast())
                .build();
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        User currentUser = securityUtils.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (!comment.getPost().getId().equals(postId)) {
            throw new ResourceNotFoundException("Comment", commentId);
        }

        // Allow deletion by comment author or post author
        if (!comment.getUser().getId().equals(currentUser.getId())
                && !post.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot delete this comment");
        }

        commentRepository.delete(comment);
        log.info("Comment {} deleted from post {} by user {}", commentId, postId, currentUser.getUsername());
    }

    private PostResponse mapPostWithStats(Post post, User currentUser) {
        int likeCount = postLikeRepository.countByPost(post);
        int commentCount = commentRepository.countByPost(post);
        boolean likedByCurrentUser = postLikeRepository.existsByPostAndUser(post, currentUser);

        return postMapper.toResponse(post, likeCount, commentCount, likedByCurrentUser);
    }
}
