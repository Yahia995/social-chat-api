package com.socialchat.service;

import com.socialchat.dto.common.PageResponse;
import com.socialchat.dto.friend.FriendRequestResponse;
import com.socialchat.dto.friend.FriendResponse;
import com.socialchat.dto.friend.RelationshipResponse;
import com.socialchat.entity.FriendRequest;
import com.socialchat.entity.FriendRequest.FriendRequestStatus;
import com.socialchat.entity.User;
import com.socialchat.exception.BadRequestException;
import com.socialchat.exception.ConflictException;
import com.socialchat.exception.ForbiddenException;
import com.socialchat.exception.ResourceNotFoundException;
import com.socialchat.mapper.FriendMapper;
import com.socialchat.repository.FriendRequestRepository;
import com.socialchat.repository.UserRepository;
import com.socialchat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final FriendMapper friendMapper;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Transactional
    public FriendRequestResponse sendFriendRequest(Long targetUserId) {
        User currentUser = securityUtils.getCurrentUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        if (currentUser.getId().equals(targetUserId)) {
            throw new BadRequestException("Cannot send friend request to yourself");
        }

        // Check if blocked
        if (friendRequestRepository.isBlocked(currentUser, targetUser)) {
            throw new BadRequestException("Cannot send friend request to this user");
        }

        // Check for existing request
        Optional<FriendRequest> existingRequest = friendRequestRepository.findAnyRequestBetweenUsers(currentUser, targetUser);
        if (existingRequest.isPresent()) {
            FriendRequest request = existingRequest.get();
            if (request.getStatus() == FriendRequestStatus.PENDING) {
                throw new ConflictException("Friend request already pending");
            } else if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
                throw new ConflictException("Already friends");
            }
        }

        FriendRequest request = FriendRequest.builder()
                .sender(currentUser)
                .receiver(targetUser)
                .status(FriendRequestStatus.PENDING)
                .build();

        request = friendRequestRepository.save(request);
        log.info("Friend request sent from {} to {}", currentUser.getUsername(), targetUser.getUsername());

        // Send notification
        notificationService.createFriendRequestNotification(targetUser, currentUser, request.getId());

        return friendMapper.toRequestResponse(request);
    }

    @Transactional
    public void acceptFriendRequest(Long requestId) {
        User currentUser = securityUtils.getCurrentUser();
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request", requestId));

        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot accept this request");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("Request is not pending");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        log.info("Friend request accepted: {} and {} are now friends", 
                request.getSender().getUsername(), request.getReceiver().getUsername());

        // Notify sender
        notificationService.createFriendAcceptedNotification(request.getSender(), currentUser);
    }

    @Transactional
    public void rejectFriendRequest(Long requestId) {
        User currentUser = securityUtils.getCurrentUser();
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request", requestId));

        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot reject this request");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("Request is not pending");
        }

        request.setStatus(FriendRequestStatus.REJECTED);
        friendRequestRepository.save(request);

        log.info("Friend request rejected by {}", currentUser.getUsername());
    }

    public PageResponse<FriendResponse> getFriends(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<FriendRequest> friendships = friendRequestRepository.findAcceptedFriendshipsPaged(currentUser, pageable);

        return PageResponse.<FriendResponse>builder()
                .content(friendships.getContent().stream()
                        .map(fr -> {
                            User friend = fr.getSender().getId().equals(currentUser.getId()) 
                                    ? fr.getReceiver() 
                                    : fr.getSender();
                            return friendMapper.toFriendResponse(friend, fr.getUpdatedAt());
                        })
                        .collect(Collectors.toList()))
                .page(friendships.getNumber())
                .size(friendships.getSize())
                .totalElements(friendships.getTotalElements())
                .totalPages(friendships.getTotalPages())
                .first(friendships.isFirst())
                .last(friendships.isLast())
                .build();
    }

    public PageResponse<FriendRequestResponse> getReceivedRequests(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<FriendRequest> pending = friendRequestRepository.findPendingRequestsReceivedPaged(currentUser, pageable);
        
        return PageResponse.<FriendRequestResponse>builder()
                .content(pending.getContent().stream()
                        .map(friendMapper::toRequestResponse)
                        .collect(Collectors.toList()))
                .page(pending.getNumber())
                .size(pending.getSize())
                .totalElements(pending.getTotalElements())
                .totalPages(pending.getTotalPages())
                .first(pending.isFirst())
                .last(pending.isLast())
                .build();
    }

    public PageResponse<FriendRequestResponse> getSentRequests(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<FriendRequest> sent = friendRequestRepository.findPendingRequestsSentPaged(currentUser, pageable);
        
        return PageResponse.<FriendRequestResponse>builder()
                .content(sent.getContent().stream()
                        .map(friendMapper::toRequestResponse)
                        .collect(Collectors.toList()))
                .page(sent.getNumber())
                .size(sent.getSize())
                .totalElements(sent.getTotalElements())
                .totalPages(sent.getTotalPages())
                .first(sent.isFirst())
                .last(sent.isLast())
                .build();
    }

    @Transactional
    public void removeFriend(Long friendId) {
        User currentUser = securityUtils.getCurrentUser();
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ResourceNotFoundException("User", friendId));

        FriendRequest friendship = friendRequestRepository.findAnyRequestBetweenUsers(currentUser, friend)
                .filter(fr -> fr.getStatus() == FriendRequestStatus.ACCEPTED)
                .orElseThrow(() -> new BadRequestException("Not friends with this user"));

        friendRequestRepository.delete(friendship);
        log.info("Friendship removed between {} and {}", currentUser.getUsername(), friend.getUsername());
    }

    @Transactional
    public void blockUser(Long userId) {
        User currentUser = securityUtils.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("Cannot block yourself");
        }

        // Remove any existing relationship
        friendRequestRepository.findAnyRequestBetweenUsers(currentUser, targetUser)
                .ifPresent(friendRequestRepository::delete);

        // Create block record
        FriendRequest block = FriendRequest.builder()
                .sender(currentUser)
                .receiver(targetUser)
                .status(FriendRequestStatus.BLOCKED)
                .build();

        friendRequestRepository.save(block);
        log.info("User {} blocked user {}", currentUser.getUsername(), targetUser.getUsername());
    }

    @Transactional
    public void unblockUser(Long userId) {
        User currentUser = securityUtils.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        FriendRequest block = friendRequestRepository.findBlockBetweenUsers(currentUser, targetUser)
                .orElseThrow(() -> new BadRequestException("User is not blocked"));

        friendRequestRepository.delete(block);
        log.info("User {} unblocked user {}", currentUser.getUsername(), targetUser.getUsername());
    }

    public RelationshipResponse getRelationship(Long userId) {
        User currentUser = securityUtils.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (currentUser.getId().equals(userId)) {
            return RelationshipResponse.builder()
                    .status("self")
                    .build();
        }

        Optional<FriendRequest> request = friendRequestRepository.findAnyRequestBetweenUsers(currentUser, targetUser);

        if (request.isEmpty()) {
            return RelationshipResponse.builder()
                    .status("none")
                    .build();
        }

        FriendRequest fr = request.get();
        String status;

        switch (fr.getStatus()) {
            case ACCEPTED -> status = "friends";
            case BLOCKED -> status = fr.getSender().getId().equals(currentUser.getId()) ? "blocked" : "blocked_by";
            case PENDING -> {
                if (fr.getSender().getId().equals(currentUser.getId())) {
                    status = "request_sent";
                } else {
                    status = "request_received";
                }
            }
            default -> status = "none";
        }

        return RelationshipResponse.builder()
                .status(status)
                .requestId(fr.getStatus() == FriendRequestStatus.PENDING ? fr.getId() : null)
                .build();
    }
}
