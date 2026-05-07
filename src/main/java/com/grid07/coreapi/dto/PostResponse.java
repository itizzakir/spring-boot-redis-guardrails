package com.grid07.coreapi.dto;

import com.grid07.coreapi.model.ActorType;
import com.grid07.coreapi.model.Post;
import java.time.Instant;

public record PostResponse(
        Long id,
        ActorType authorType,
        Long authorId,
        String content,
        Instant createdAt,
        long viralityScore
) {
    public static PostResponse from(Post post, long viralityScore) {
        return new PostResponse(
                post.getId(),
                post.getAuthorType(),
                post.getAuthorId(),
                post.getContent(),
                post.getCreatedAt(),
                viralityScore
        );
    }
}
