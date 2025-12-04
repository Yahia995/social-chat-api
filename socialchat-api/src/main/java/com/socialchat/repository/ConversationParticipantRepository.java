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
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);

    boolean existsByConversationAndUser(Conversation conversation, User user);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastReadAt = :readAt WHERE cp.conversation = :conversation AND cp.user = :user")
    void updateLastReadAt(@Param("conversation") Conversation conversation, @Param("user") User user, @Param("readAt") LocalDateTime readAt);
}
