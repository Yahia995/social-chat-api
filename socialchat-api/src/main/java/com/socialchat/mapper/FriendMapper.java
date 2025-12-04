package com.socialchat.mapper;

import com.socialchat.dto.friend.FriendRequestResponse;
import com.socialchat.dto.friend.FriendResponse;
import com.socialchat.entity.FriendRequest;
import com.socialchat.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class})
public interface FriendMapper {

    FriendRequestResponse toRequestResponse(FriendRequest request);

    @Mapping(target = "user", source = "user")
    @Mapping(target = "friendsSince", source = "friendsSince")
    FriendResponse toFriendResponse(User user, LocalDateTime friendsSince);
}
