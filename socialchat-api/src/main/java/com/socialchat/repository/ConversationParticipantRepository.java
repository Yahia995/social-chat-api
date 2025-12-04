package com.socialchat.repository;

import com.socialchat.entity.Conversation;
import com.socialchat.entity.ConversationParticipant;
import com.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);

    boolean existsByConversationAndUser(Conversation conversation, User user);

    @Query("SELECT COUNT(cp) > 0 FROM ConversationParticipant cp " +
            "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    boolean existsByConversationIdAndUserId(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    @Query("SELECT cp.user.id FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId")
    List<Long> findUserIdsByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastReadAt = :readAt WHERE cp.conversation = :conversation AND cp.user = :user")
    void updateLastReadAt(@Param("conversation") Conversation conversation, @Param("user") User user, @Param("readAt") LocalDateTime readAt);
}
