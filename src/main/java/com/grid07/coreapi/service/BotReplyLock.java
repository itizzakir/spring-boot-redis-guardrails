package com.grid07.coreapi.service;

public record BotReplyLock(
        Long postId,
        Long botId,
        Long humanId,
        boolean cooldownReserved
) {
}
