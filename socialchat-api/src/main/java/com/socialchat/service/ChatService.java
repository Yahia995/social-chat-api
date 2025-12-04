package com.socialchat.service;

import com.socialchat.dto.chat.*;
import com.socialchat.dto.common.PageResponse;
import com.socialchat.dto.user.UserResponse;
import com.socialchat.entity.*;
import com.socialchat.exception.BadRequestException;
import com.socialchat.exception.ForbiddenException;
import com.socialchat.exception.ResourceNotFoundException;
import com.socialchat.mapper.ChatMapper;
import com.socialchat.mapper.UserMapper;
import com.socialchat.repository.*;
import com.socialchat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final WebSocketService webSocketService;
    private final SecurityUtils securityUtils;

    @Transactional
    public ConversationResponse createOrGetConversation(ConversationRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        List<Long> participantIds = request.getParticipantIds();
        if (!participantIds.contains(currentUser.getId())) {
            participantIds.add(currentUser.getId());
        }

        if (participantIds.size() < 2) {
            throw new BadRequestException("Conversation must have at least 2 participants");
        }

        // Check for existing direct conversation
        if (participantIds.size() == 2 && (request.getName() == null || request.getName().isEmpty())) {
            Long otherUserId = participantIds.stream()
                    .filter(id -> !id.equals(currentUser.getId()))
                    .findFirst()
                    .orElseThrow();

            Optional<Conversation> existing = conversationRepository.findDirectConversation(currentUser.getId(), otherUserId);
            if (existing.isPresent()) {
                return mapConversationResponse(existing.get(), currentUser);
            }
        }

        boolean isGroup = participantIds.size() > 2 || (request.getName() != null && !request.getName().isEmpty());

        Conversation conversation = Conversation.builder()
                .name(request.getName())
                .isGroup(isGroup)
                .build();

        conversation = conversationRepository.save(conversation);

        for (Long userId : participantIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));

            ConversationParticipant participant = ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(user)
                    .joinedAt(LocalDateTime.now())
                    .lastReadAt(LocalDateTime.now())
                    .build();

            participantRepository.save(participant);
        }

        log.info("Conversation created with {} participants", participantIds.size());

        return mapConversationResponse(conversation, currentUser);
    }

    public PageResponse<ConversationResponse> getConversations(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<Conversation> conversations = conversationRepository.findByUserIdPaged(currentUser.getId(), pageable);

        List<ConversationResponse> content = conversations.getContent().stream()
                .map(conv -> mapConversationResponse(conv, currentUser))
                .collect(Collectors.toList());

        return PageResponse.<ConversationResponse>builder()
                .content(content)
                .page(conversations.getNumber())
                .size(conversations.getSize())
                .totalElements(conversations.getTotalElements())
                .totalPages(conversations.getTotalPages())
                .first(conversations.isFirst())
                .last(conversations.isLast())
                .build();
    }

    public ConversationResponse getConversation(Long conversationId) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        validateParticipant(conversation, currentUser);

        return mapConversationResponse(conversation, currentUser);
    }

    public PageResponse<MessageResponse> getMessages(Long conversationId, Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        validateParticipant(conversation, currentUser);

        Page<Message> messages = messageRepository.findByConversationOrderByCreatedAtDesc(conversation, pageable);

        List<MessageResponse> content = messages.getContent().stream()
                .map(chatMapper::toMessageResponse)
                .collect(Collectors.toList());

        return PageResponse.<MessageResponse>builder()
                .content(content)
                .page(messages.getNumber())
                .size(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .first(messages.isFirst())
                .last(messages.isLast())
                .build();
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, MessageRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        return sendMessageInternal(conversationId, currentUser, request);
    }

    @Transactional
    public MessageResponse sendMessageFromWebSocket(Long conversationId, Long userId, MessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return sendMessageInternal(conversationId, user, request);
    }

    private MessageResponse sendMessageInternal(Long conversationId, User sender, MessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        validateParticipant(conversation, sender);

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .build();

        message = messageRepository.save(message);

        // Update conversation timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        MessageResponse response = chatMapper.toMessageResponse(message);

        // Send via WebSocket to all participants
        webSocketService.sendChatMessage(conversationId, response);

        log.info("Message sent in conversation {} by user {}", conversationId, sender.getUsername());

        return response;
    }

    @Transactional
    public void markConversationAsRead(Long conversationId) {
        User currentUser = securityUtils.getCurrentUser();
        markConversationAsReadInternal(conversationId, currentUser);
    }

    @Transactional
    public void markConversationAsReadFromWebSocket(Long conversationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        markConversationAsReadInternal(conversationId, user);
    }

    private void markConversationAsReadInternal(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        validateParticipant(conversation, user);

        LocalDateTime now = LocalDateTime.now();
        participantRepository.updateLastReadAt(conversation, user, now);

        // Notify via WebSocket
        webSocketService.sendReadReceipt(conversationId, user.getId(), now);

        log.debug("Messages marked as read in conversation {} by user {}", conversationId, user.getUsername());
    }

    @Transactional
    public void leaveConversation(Long conversationId) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        ConversationParticipant participant = participantRepository.findByConversationAndUser(conversation, currentUser)
                .orElseThrow(() -> new ForbiddenException("Not a participant of this conversation"));

        participantRepository.delete(participant);

        log.info("User {} left conversation {}", currentUser.getUsername(), conversationId);
    }

    private void validateParticipant(Conversation conversation, User user) {
        boolean isParticipant = participantRepository.existsByConversationAndUser(conversation, user);
        if (!isParticipant) {
            throw new ForbiddenException("Not a participant of this conversation");
        }
    }

    private ConversationResponse mapConversationResponse(Conversation conversation, User currentUser) {
        List<UserResponse> participants = conversation.getParticipants().stream()
                .map(p -> userMapper.toResponse(p.getUser()))
                .collect(Collectors.toList());

        Message lastMessage = messageRepository.findTopByConversationOrderByCreatedAtDesc(conversation)
                .orElse(null);

        long unreadCount = 0;
        Optional<ConversationParticipant> currentParticipant = participantRepository.findByConversationAndUser(conversation, currentUser);
        if (currentParticipant.isPresent() && lastMessage != null) {
            LocalDateTime lastRead = currentParticipant.get().getLastReadAt();
            if (lastRead != null) {
                unreadCount = messageRepository.countByConversationAndCreatedAtAfter(conversation, lastRead);
            }
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .name(conversation.getName())
                .isGroup(conversation.getIsGroup())
                .participants(participants)
                .lastMessage(lastMessage != null ? chatMapper.toMessageResponse(lastMessage) : null)
                .unreadCount(unreadCount)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
