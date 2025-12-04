package com.socialchat.repository;

import com.socialchat.entity.Conversation;
import com.socialchat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);

    Optional<Message> findTopByConversationOrderByCreatedAtDesc(Conversation conversation);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conversation AND m.createdAt > :since AND m.sender.id != :userId")
    int countUnreadMessages(@Param("conversation") Conversation conversation, @Param("since") LocalDateTime since, @Param("userId") Long userId);

    long countByConversationAndCreatedAtAfter(Conversation conversation, LocalDateTime after);
}
