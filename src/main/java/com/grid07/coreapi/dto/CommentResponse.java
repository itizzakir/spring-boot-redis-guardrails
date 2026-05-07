package com.grid07.coreapi.dto;

import com.grid07.coreapi.model.ActorType;
import com.grid07.coreapi.model.Comment;
import java.time.Instant;

public record CommentResponse(
        Long id,
        Long postId,
        Long parentCommentId,
        ActorType authorType,
        Long authorId,
        String content,
        int depthLevel,
        Instant createdAt
) {
    public static CommentResponse from(Comment comment) {
        Long parentId = comment.getParentComment() == null ? null : comment.getParentComment().getId();
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                parentId,
                comment.getAuthorType(),
                comment.getAuthorId(),
                comment.getContent(),
                comment.getDepthLevel(),
                comment.getCreatedAt()
        );
    }
}
