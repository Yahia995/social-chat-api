package com.socialchat.mapper;

import com.socialchat.dto.friend.FriendRequestResponse;
import com.socialchat.dto.friend.FriendResponse;
import com.socialchat.entity.FriendRequest;
import com.socialchat.entity.User;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T10:18:08+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class FriendMapperImpl implements FriendMapper {

    @Autowired
    private UserMapper userMapper;

    @Override
    public FriendRequestResponse toRequestResponse(FriendRequest request) {
        if ( request == null ) {
            return null;
        }

        FriendRequestResponse.FriendRequestResponseBuilder friendRequestResponse = FriendRequestResponse.builder();

        friendRequestResponse.id( request.getId() );
        friendRequestResponse.sender( userMapper.toResponse( request.getSender() ) );
        friendRequestResponse.receiver( userMapper.toResponse( request.getReceiver() ) );
        friendRequestResponse.status( request.getStatus() );
        friendRequestResponse.createdAt( request.getCreatedAt() );

        return friendRequestResponse.build();
    }

    @Override
    public FriendResponse toFriendResponse(User user, LocalDateTime friendsSince) {
        if ( user == null && friendsSince == null ) {
            return null;
        }

        FriendResponse.FriendResponseBuilder friendResponse = FriendResponse.builder();

        friendResponse.user( userMapper.toResponse( user ) );
        friendResponse.friendsSince( friendsSince );

        return friendResponse.build();
    }
}
