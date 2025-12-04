package com.socialchat.mapper;

import com.socialchat.dto.post.CommentResponse;
import com.socialchat.dto.post.PostResponse;
import com.socialchat.entity.Comment;
import com.socialchat.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class})
public interface PostMapper {

    @Mapping(target = "likeCount", source = "likeCount")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(target = "likedByCurrentUser", source = "likedByCurrentUser")
    PostResponse toResponse(Post post, int likeCount, int commentCount, boolean likedByCurrentUser);

    CommentResponse toCommentResponse(Comment comment);
}
