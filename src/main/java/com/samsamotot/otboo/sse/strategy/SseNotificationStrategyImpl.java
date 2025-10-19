package com.samsamotot.otboo.sse.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.sse.transport.MemorySseNotificationTransport;
import com.samsamotot.otboo.sse.transport.RedisSseNotificationTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.strategy
 * FileName     : SseNotificationStrategyImpl
 * Author       : dounguk
 * Date         : 2025. 10. 18.
 * Description  : SSE 알림 전송 전략 구현체
 *                Kafka -> Redis -> Memory 순으로 Fallback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseNotificationStrategyImpl implements SseNotificationStrategy {

    private static final String SSE_NOTIFICATION_STRATEGY_IMPL = "[SseNotificationStrategyImpl] ";

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    @Qualifier("kafkaTemplate")
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired(required = false)
    private RedisSseNotificationTransport redisSseNotificationTransport;

    @Autowired(required = false)
    private MemorySseNotificationTransport memorySseNotificationTransport;

    /**
     * 메시징 전략을 결정하고 메시지를 발행합니다.
     *
     * @param userId 사용자 ID
     * @param notification 알림 데이터
     */
    @Override
    public void publishNotification(UUID userId, NotificationDto notification) {
        try {
            String notificationData = objectMapper.writeValueAsString(notification);

            // 디버그 로그 추가
            log.info(SSE_NOTIFICATION_STRATEGY_IMPL + "알림 발행 시작 - userId: {}", userId);
            log.info(SSE_NOTIFICATION_STRATEGY_IMPL + "Kafka 사용 가능: {}, Redis 사용 가능: {}", isKafkaAvailable(), isRedisAvailable());

            if (isKafkaAvailable()) {
                log.info(SSE_NOTIFICATION_STRATEGY_IMPL + "Kafka 전략 선택 - userId: {}", userId);
                publishViaKafka(userId, notificationData);
            } else if (isRedisAvailable()) {
                log.info(SSE_NOTIFICATION_STRATEGY_IMPL + "Redis 전략 선택 - userId: {}", userId);
                publishViaRedis(userId, notificationData);
            } else {
                log.info(SSE_NOTIFICATION_STRATEGY_IMPL + "Memory 전략 선택 - userId: {}", userId);
                publishViaMemory(userId, notificationData);
            }
        } catch (Exception e) {
            log.error(SSE_NOTIFICATION_STRATEGY_IMPL + "메시지 발행 실패 - userId: {}", userId, e);
            // 최후의 수단으로 메모리 방식 사용
            try {
                publishViaMemory(userId, notification.toString());
            } catch (Exception ex) {
                log.error(SSE_NOTIFICATION_STRATEGY_IMPL + "메모리 발행도 실패 - userId: {}", userId, ex);
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
        return redisSseNotificationTransport != null && redisSseNotificationTransport.isAvailable();
    }

    /**
     * Kafka를 통한 메시지 발행
     */
    private void publishViaKafka(UUID userId, String notificationData) {
        if (kafkaTemplate == null) {
            log.warn(SSE_NOTIFICATION_STRATEGY_IMPL + "KafkaTemplate이 null입니다. Redis로 Fallback - userId: {}", userId);
            publishViaRedis(userId, notificationData);
            return;
        }
        
        try {
            String topic = "sse-notifications";
            String key = userId.toString();

            kafkaTemplate.send(topic, key, notificationData)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info(SSE_NOTIFICATION_STRATEGY_IMPL + "Kafka 발행 성공 - userId: {}, partition: {}, offset: {}",
                                 userId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.warn(SSE_NOTIFICATION_STRATEGY_IMPL + "Kafka 발행 실패, Redis로 Fallback - userId: {}", userId);
                        publishViaRedis(userId, notificationData);
                    }
                });
        } catch (Exception e) {
            log.warn(SSE_NOTIFICATION_STRATEGY_IMPL + "Kafka 발행 실패, Redis로 Fallback - userId: {}", userId, e);
            publishViaRedis(userId, notificationData);
        }
    }

    /**
     * Redis를 통한 메시지 발행
     */
    private void publishViaRedis(UUID userId, String notificationData) {
        try {
            if (redisSseNotificationTransport != null && redisSseNotificationTransport.isAvailable()) {
                redisSseNotificationTransport.publishNotification(userId, notificationData);
                log.info(SSE_NOTIFICATION_STRATEGY_IMPL + "Redis 발행 성공 - userId: {}", userId);
            } else {
                log.warn(SSE_NOTIFICATION_STRATEGY_IMPL + "Redis 사용 불가, Memory로 Fallback - userId: {}", userId);
                publishViaMemory(userId, notificationData);
            }
        } catch (Exception e) {
            log.warn(SSE_NOTIFICATION_STRATEGY_IMPL + "Redis 발행 실패, Memory로 Fallback - userId: {}", userId, e);
            publishViaMemory(userId, notificationData);
        }
    }

    /**
     * 메모리를 통한 메시지 발행 (최후의 수단)
     */
    private void publishViaMemory(UUID userId, String notificationData) {
        try {
            if (memorySseNotificationTransport != null) {
                memorySseNotificationTransport.publishNotification(userId, notificationData);
                log.info(SSE_NOTIFICATION_STRATEGY_IMPL + "Memory 발행 성공 - userId: {}", userId);
            } else {
                log.error(SSE_NOTIFICATION_STRATEGY_IMPL + "모든 메시징 방식 실패 - userId: {}", userId);
            }
        } catch (Exception e) {
            log.error(SSE_NOTIFICATION_STRATEGY_IMPL + "Memory 발행 실패 - userId: {}", userId, e);
        }
    }
}
