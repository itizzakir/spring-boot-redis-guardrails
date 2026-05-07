package com.grid07.coreapi.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ViralityService {

    private final StringRedisTemplate redisTemplate;

    public ViralityService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long incrementScore(Long postId, InteractionType type) {
        Long score = redisTemplate.opsForValue().increment(viralityKey(postId), type.scoreDelta());
        return score == null ? 0L : score;
    }

    public long getScore(Long postId) {
        String score = redisTemplate.opsForValue().get(viralityKey(postId));
        return score == null ? 0L : Long.parseLong(score);
    }

    private String viralityKey(Long postId) {
        return "post:%d:virality_score".formatted(postId);
    }
}
