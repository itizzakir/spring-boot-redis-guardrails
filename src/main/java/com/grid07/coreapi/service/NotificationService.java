package com.grid07.coreapi.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final Duration USER_NOTIFICATION_COOLDOWN = Duration.ofMinutes(15);
    private static final String PENDING_USERS_KEY = "users:pending_notifications";

    private final StringRedisTemplate redisTemplate;

    public NotificationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void handleBotInteractionOnUserPost(Long userId, String botName) {
        String cooldownKey = notificationCooldownKey(userId);
        String pendingKey = pendingListKey(userId);
        String notification = botName + " replied to your post";

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            redisTemplate.opsForList().rightPush(pendingKey, notification);
            redisTemplate.opsForSet().add(PENDING_USERS_KEY, userId.toString());
            return;
        }

        log.info("Push Notification Sent to User {}: {}", userId, notification);
        redisTemplate.opsForValue().set(cooldownKey, "1", USER_NOTIFICATION_COOLDOWN);
    }

    @Scheduled(fixedRateString = "${app.notifications.sweep-rate-ms:300000}")
    public void sweepPendingNotifications() {
        Set<String> userIds = redisTemplate.opsForSet().members(PENDING_USERS_KEY);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (String userId : userIds) {
            summarizeAndClear(userId);
        }
    }

    private void summarizeAndClear(String userId) {
        String pendingKey = pendingListKey(userId);
        List<String> messages = redisTemplate.opsForList().range(pendingKey, 0, -1);
        redisTemplate.delete(pendingKey);
        redisTemplate.opsForSet().remove(PENDING_USERS_KEY, userId);

        if (messages == null || messages.isEmpty()) {
            return;
        }

        String firstBot = botNameFrom(messages.get(0));
        int others = Math.max(messages.size() - 1, 0);
        log.info("Summarized Push Notification for User {}: {} and {} others interacted with your posts.",
                userId,
                firstBot,
                others);
    }

    private String botNameFrom(String notification) {
        int repliedIndex = notification.indexOf(" replied");
        if (repliedIndex == -1) {
            return notification;
        }
        return notification.substring(0, repliedIndex);
    }

    private String notificationCooldownKey(Long userId) {
        return "user:%d:notif_cooldown".formatted(userId);
    }

    private String pendingListKey(Object userId) {
        return "user:%s:pending_notifs".formatted(userId);
    }
}
