package com.socialchat.mapper;

import com.socialchat.dto.chat.ConversationResponse;
import com.socialchat.dto.chat.MessageResponse;
import com.socialchat.dto.user.UserResponse;
import com.socialchat.entity.Conversation;
import com.socialchat.entity.Message;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T10:18:08+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class ChatMapperImpl implements ChatMapper {

    @Autowired
    private UserMapper userMapper;

    @Override
    public MessageResponse toMessageResponse(Message message) {
        if ( message == null ) {
            return null;
        }

        MessageResponse.MessageResponseBuilder messageResponse = MessageResponse.builder();

        messageResponse.conversationId( messageConversationId( message ) );
        messageResponse.id( message.getId() );
        messageResponse.sender( userMapper.toResponse( message.getSender() ) );
        messageResponse.content( message.getContent() );
        messageResponse.imageUrl( message.getImageUrl() );
        messageResponse.createdAt( message.getCreatedAt() );

        return messageResponse.build();
    }

    @Override
    public ConversationResponse toConversationResponse(Conversation conversation, List<UserResponse> participants, MessageResponse lastMessage, Integer unreadCount) {
        if ( conversation == null && participants == null && lastMessage == null && unreadCount == null ) {
            return null;
        }

        ConversationResponse.ConversationResponseBuilder conversationResponse = ConversationResponse.builder();

        if ( conversation != null ) {
            conversationResponse.id( conversation.getId() );
            conversationResponse.createdAt( conversation.getCreatedAt() );
            conversationResponse.name( conversation.getName() );
            conversationResponse.isGroup( conversation.getIsGroup() );
            conversationResponse.updatedAt( conversation.getUpdatedAt() );
        }
        List<UserResponse> list = participants;
        if ( list != null ) {
            conversationResponse.participants( new ArrayList<UserResponse>( list ) );
        }
        conversationResponse.lastMessage( lastMessage );
        if ( unreadCount != null ) {
            conversationResponse.unreadCount( unreadCount.longValue() );
        }

        return conversationResponse.build();
    }

    private Long messageConversationId(Message message) {
        if ( message == null ) {
            return null;
        }
        Conversation conversation = message.getConversation();
        if ( conversation == null ) {
            return null;
        }
        Long id = conversation.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
