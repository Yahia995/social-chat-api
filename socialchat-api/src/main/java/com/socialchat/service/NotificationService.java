package com.socialchat.service;

import com.socialchat.dto.common.PageResponse;
import com.socialchat.dto.notification.NotificationResponse;
import com.socialchat.dto.websocket.NotificationEvent;
import com.socialchat.entity.Notification;
import com.socialchat.entity.User;
import com.socialchat.exception.ForbiddenException;
import com.socialchat.exception.ResourceNotFoundException;
import com.socialchat.mapper.NotificationMapper;
import com.socialchat.repository.NotificationRepository;
import com.socialchat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final WebSocketService webSocketService;
    private final SecurityUtils securityUtils;

    @Transactional
    public void createNotification(User recipient, String type, String title, String message, String data) {
        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", recipient.getUsername(), type);

        NotificationEvent event = NotificationEvent.builder()
                .id(notification.getId())
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .createdAt(notification.getCreatedAt())
                .build();

        webSocketService.sendNotificationToUser(recipient.getUsername(), event);
    }

    public void createFriendRequestNotification(User recipient, User sender, Long requestId) {
        String data = String.format("{\"requestId\": %d, \"senderId\": %d}", requestId, sender.getId());
        createNotification(recipient, "FRIEND_REQUEST", "Friend Request",
                sender.getDisplayName() + " sent you a friend request", data);
    }

    public void createFriendAcceptedNotification(User recipient, User accepter) {
        String data = String.format("{\"userId\": %d}", accepter.getId());
        createNotification(recipient, "FRIEND_ACCEPTED", "Friend Request Accepted",
                accepter.getDisplayName() + " accepted your friend request", data);
    }

    public PageResponse<NotificationResponse> getNotifications(Pageable pageable) {
        User user = securityUtils.getCurrentUser();
        Page<Notification> page = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return PageResponse.<NotificationResponse>builder()
                .content(page.getContent().stream()
                        .map(notificationMapper::toResponse)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public long getUnreadCount() {
        User user = securityUtils.getCurrentUser();
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        User user = securityUtils.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot mark notification as read");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User user = securityUtils.getCurrentUser();
        notificationRepository.markAllAsReadByUserId(user.getId());
        log.info("Marked all notifications as read for user: {}", user.getUsername());
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        User user = securityUtils.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot delete notification");
        }

        notificationRepository.delete(notification);
    }
}
