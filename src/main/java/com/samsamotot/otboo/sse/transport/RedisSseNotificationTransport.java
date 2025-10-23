package com.samsamotot.otboo.sse.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.transport
 * FileName     : RedisSseNotificationService
 * Author       : dounguk
 * Date         : 2025. 10. 18.
 * Description  : Redis Pub/Sub를 통한 SSE 알림 전송 서비스 (Fallback용)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSseNotificationTransport {

    private static final String REDIS_SSE_NOTIFICATION_TRANSPORT = "[RedisSseNotificationTransport] ";

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis 사용 가능 여부 확인
     */
    public boolean isAvailable() {
        if (redisTemplate == null) {
            return false;
        }

        try {
            // Redis 연결 테스트
            try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
                connection.ping();
            }
            return true;

        } catch (Exception e) {
            log.warn(REDIS_SSE_NOTIFICATION_TRANSPORT + "Redis 연결 불가: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Redis Pub/Sub를 통한 SSE 알림 발행
     */
    public void publishNotification(UUID userId, String notificationData) {
        if (!isAvailable()) {
            throw new RuntimeException("레디스 사용 불가");
        }

        try {
            String channel = "sse:notification:" + userId.toString();
            redisTemplate.convertAndSend(channel, notificationData);
            
            log.info(REDIS_SSE_NOTIFICATION_TRANSPORT + "Redis 발행 성공 - userId: {}, channel: {}", userId, channel);
        } catch (Exception e) {
            log.error(REDIS_SSE_NOTIFICATION_TRANSPORT + "Redis 발행 실패 - userId: {}", userId, e);
            throw e;
        }
    }
}
