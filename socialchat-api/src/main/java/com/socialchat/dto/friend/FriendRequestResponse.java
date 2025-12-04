package com.socialchat.dto.friend;

import com.socialchat.dto.user.UserResponse;
import com.socialchat.entity.FriendRequest.FriendRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponse {
    private Long id;
    private UserResponse sender;
    private UserResponse receiver;
    private FriendRequestStatus status;
    private LocalDateTime createdAt;
}
