package com.samsamotot.otboo.sse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.service
 * FileName     : MessagingStrategyService
 * Author       : dounguk
 * Date         : 2025. 10. 17.
 * Description  : 메시징 전략을 결정하고 적절한 방식으로 메시지를 발행하는 서비스
 *                Kafka -> Redis -> Memory 순으로 Fallback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingStrategyService {

    private static final String MESSAGING_STRATEGY = "[MessagingStrategy] ";

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    @Qualifier("kafkaTemplate")
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired(required = false)
    private RedisMessagingService redisMessagingService;

    @Autowired(required = false)
    private MemoryMessagingService memoryMessagingService;

    /**
     * 메시징 전략을 결정하고 메시지를 발행합니다.
     * 
     * @param userId 사용자 ID
     * @param notification 알림 데이터
     */
    public void publishNotification(UUID userId, NotificationDto notification) {
        try {
            String notificationData = objectMapper.writeValueAsString(notification);
            
            if (isKafkaAvailable()) {
                publishViaKafka(userId, notificationData);
            } else if (isRedisAvailable()) {
                publishViaRedis(userId, notificationData);
            } else {
                publishViaMemory(userId, notificationData);
            }
        } catch (Exception e) {
            log.error(MESSAGING_STRATEGY + "메시지 발행 실패 - userId: {}", userId, e);
            // 최후의 수단으로 메모리 방식 사용
            try {
                publishViaMemory(userId, notification.toString());
            } catch (Exception ex) {
                log.error(MESSAGING_STRATEGY + "메모리 발행도 실패 - userId: {}", userId, ex);
            }
        }
    }

    /**
     * Kafka 사용 가능 여부 확인
     */
    private boolean isKafkaAvailable() {
        return kafkaTemplate != null;
    }

    /**
     * Redis 사용 가능 여부 확인
     */
    private boolean isRedisAvailable() {
        return redisMessagingService != null && redisMessagingService.isAvailable();
    }

    /**
     * Kafka를 통한 메시지 발행
     */
    private void publishViaKafka(UUID userId, String notificationData) {
        try {
            String topic = "sse-notifications";
            String key = userId.toString();

            kafkaTemplate.send(topic, key, notificationData)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info(MESSAGING_STRATEGY + "Kafka 발행 성공 - userId: {}, partition: {}, offset: {}",
                                 userId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.warn(MESSAGING_STRATEGY + "Kafka 발행 실패, Redis로 Fallback - userId: {}", userId);
                        publishViaRedis(userId, notificationData);
                    }
                });
        } catch (Exception e) {
            log.warn(MESSAGING_STRATEGY + "Kafka 발행 실패, Redis로 Fallback - userId: {}", userId, e);
            publishViaRedis(userId, notificationData);
        }
    }

    /**
     * Redis를 통한 메시지 발행
     */
    private void publishViaRedis(UUID userId, String notificationData) {
        try {
            if (redisMessagingService != null && redisMessagingService.isAvailable()) {
                redisMessagingService.publishNotification(userId, notificationData);
                log.info(MESSAGING_STRATEGY + "Redis 발행 성공 - userId: {}", userId);
            } else {
                log.warn(MESSAGING_STRATEGY + "Redis 사용 불가, Memory로 Fallback - userId: {}", userId);
                publishViaMemory(userId, notificationData);
            }
        } catch (Exception e) {
            log.warn(MESSAGING_STRATEGY + "Redis 발행 실패, Memory로 Fallback - userId: {}", userId, e);
            publishViaMemory(userId, notificationData);
        }
    }

    /**
     * 메모리를 통한 메시지 발행 (최후의 수단)
     */
    private void publishViaMemory(UUID userId, String notificationData) {
        try {
            if (memoryMessagingService != null) {
                memoryMessagingService.publishNotification(userId, notificationData);
                log.info(MESSAGING_STRATEGY + "Memory 발행 성공 - userId: {}", userId);
            } else {
                log.error(MESSAGING_STRATEGY + "모든 메시징 방식 실패 - userId: {}", userId);
            }
        } catch (Exception e) {
            log.error(MESSAGING_STRATEGY + "Memory 발행 실패 - userId: {}", userId, e);
        }
    }

    /**
     * 현재 사용 중인 메시징 전략 반환
     */
    public String getCurrentStrategy() {
        if (isKafkaAvailable()) {
            return "KAFKA";
        } else if (isRedisAvailable()) {
            return "REDIS";
        } else {
            return "MEMORY";
        }
    }
}
