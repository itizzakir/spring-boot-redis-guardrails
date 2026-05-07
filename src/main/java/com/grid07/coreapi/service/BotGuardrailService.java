package com.grid07.coreapi.service;

import com.grid07.coreapi.exception.GuardrailViolationException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BotGuardrailService {

    private static final int MAX_BOT_REPLIES_PER_POST = 100;
    private static final Duration BOT_HUMAN_COOLDOWN = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public BotGuardrailService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public BotReplyLock reserveBotReply(Long postId, Long botId, Long humanId) {
        boolean cooldownReserved = reserveCooldownIfNeeded(botId, humanId);

        /*
         * Redis INCR is atomic, so two app instances cannot read the same value.
         * Only requests that receive values 1..100 are allowed to touch the DB.
         * Values above 100 are immediately rejected and compensated with DECR.
         */
        Long count = redisTemplate.opsForValue().increment(botCountKey(postId));
        if (count == null) {
            releaseCooldown(botId, humanId, cooldownReserved);
            throw new GuardrailViolationException("Redis did not return a bot counter value");
        }

        if (count > MAX_BOT_REPLIES_PER_POST) {
            redisTemplate.opsForValue().decrement(botCountKey(postId));
            releaseCooldown(botId, humanId, cooldownReserved);
            throw new GuardrailViolationException("Horizontal cap reached: this post already has 100 bot replies");
        }

        return new BotReplyLock(postId, botId, humanId, cooldownReserved);
    }

    public void release(BotReplyLock lock) {
        if (lock == null) {
            return;
        }
        redisTemplate.opsForValue().decrement(botCountKey(lock.postId()));
        releaseCooldown(lock.botId(), lock.humanId(), lock.cooldownReserved());
    }

    private boolean reserveCooldownIfNeeded(Long botId, Long humanId) {
        if (humanId == null) {
            return false;
        }

        /*
         * SETNX with a TTL is the cooldown lock. The check and the write happen
         * as one Redis command, so concurrent requests cannot both pass it.
         */
        Boolean accepted = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey(botId, humanId), "1", BOT_HUMAN_COOLDOWN);
        if (!Boolean.TRUE.equals(accepted)) {
            throw new GuardrailViolationException("Cooldown cap reached: this bot already interacted with this human in the last 10 minutes");
        }
        return true;
    }

    private void releaseCooldown(Long botId, Long humanId, boolean cooldownReserved) {
        if (cooldownReserved && humanId != null) {
            redisTemplate.delete(cooldownKey(botId, humanId));
        }
    }

    private String botCountKey(Long postId) {
        return "post:%d:bot_count".formatted(postId);
    }

    private String cooldownKey(Long botId, Long humanId) {
        return "cooldown:bot_%d:human_%d".formatted(botId, humanId);
    }
}
