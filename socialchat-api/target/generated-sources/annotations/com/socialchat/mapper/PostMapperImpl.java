package com.socialchat.mapper;

import com.socialchat.dto.post.CommentResponse;
import com.socialchat.dto.post.PostResponse;
import com.socialchat.entity.Comment;
import com.socialchat.entity.Post;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T10:18:08+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class PostMapperImpl implements PostMapper {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PostResponse toResponse(Post post, int likeCount, int commentCount, boolean likedByCurrentUser) {
        if ( post == null ) {
            return null;
        }

        PostResponse.PostResponseBuilder postResponse = PostResponse.builder();

        if ( post != null ) {
            postResponse.id( post.getId() );
            postResponse.user( userMapper.toResponse( post.getUser() ) );
            postResponse.content( post.getContent() );
            postResponse.imageUrl( post.getImageUrl() );
            postResponse.createdAt( post.getCreatedAt() );
        }
        postResponse.likeCount( likeCount );
        postResponse.commentCount( commentCount );
        postResponse.likedByCurrentUser( likedByCurrentUser );

        return postResponse.build();
    }

    @Override
    public CommentResponse toCommentResponse(Comment comment) {
        if ( comment == null ) {
            return null;
        }

        CommentResponse.CommentResponseBuilder commentResponse = CommentResponse.builder();

        commentResponse.id( comment.getId() );
        commentResponse.user( userMapper.toResponse( comment.getUser() ) );
        commentResponse.content( comment.getContent() );
        commentResponse.createdAt( comment.getCreatedAt() );

        return commentResponse.build();
    }
}
