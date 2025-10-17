package com.samsamotot.otboo.sse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.service
 * FileName     : RedisMessagingService
 * Author       : dounguk
 * Date         : 2025. 10. 17.
 * Description  : Redis Pub/Sub를 통한 메시징 서비스 (Fallback용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessagingService {

    private static final String REDIS_MESSAGING = "[RedisMessagingService] ";

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
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn(REDIS_MESSAGING + "Redis 연결 불가: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Redis Pub/Sub를 통한 알림 발행
     */
    public void publishNotification(UUID userId, String notificationData) {
        if (!isAvailable()) {
            throw new RuntimeException("레디스 사용 불가");
        }

        try {
            String channel = "sse:notification:" + userId.toString();
            redisTemplate.convertAndSend(channel, notificationData);
            
            log.info(REDIS_MESSAGING + "Redis 발행 성공 - userId: {}, channel: {}", userId, channel);
        } catch (Exception e) {
            log.error(REDIS_MESSAGING + "Redis 발행 실패 - userId: {}", userId, e);
            throw e;
        }
    }
}
