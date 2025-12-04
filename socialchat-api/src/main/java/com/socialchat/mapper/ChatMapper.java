package com.socialchat.mapper;

import com.socialchat.dto.chat.ConversationResponse;
import com.socialchat.dto.chat.MessageResponse;
import com.socialchat.dto.user.UserResponse;
import com.socialchat.entity.Conversation;
import com.socialchat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class})
public interface ChatMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    MessageResponse toMessageResponse(Message message);

    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "participants", source = "participants")
    @Mapping(target = "lastMessage", source = "lastMessage")
    @Mapping(target = "unreadCount", source = "unreadCount")
    @Mapping(target = "createdAt", source = "conversation.createdAt")
    ConversationResponse toConversationResponse(Conversation conversation, List<UserResponse> participants, 
                                                  MessageResponse lastMessage, Integer unreadCount);
}
