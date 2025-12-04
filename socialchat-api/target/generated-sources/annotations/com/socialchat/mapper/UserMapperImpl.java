package com.socialchat.mapper;

import com.socialchat.dto.user.UserResponse;
import com.socialchat.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T10:18:08+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.id( user.getId() );
        userResponse.username( user.getUsername() );
        userResponse.email( user.getEmail() );
        userResponse.displayName( user.getDisplayName() );
        userResponse.bio( user.getBio() );
        userResponse.photoUrl( user.getPhotoUrl() );
        userResponse.online( user.getOnline() );
        userResponse.lastSeen( user.getLastSeen() );
        userResponse.createdAt( user.getCreatedAt() );

        return userResponse.build();
    }
}
