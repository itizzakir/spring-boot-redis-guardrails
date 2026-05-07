package com.grid07.coreapi.dto;

public record LikeResponse(
        Long postId,
        Long userId,
        long viralityScore,
        String message
) {
}
