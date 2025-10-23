package com.samsamotot.otboo.sse.strategy;

import com.samsamotot.otboo.notification.dto.NotificationDto;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.strategy
 * FileName     : SseNotificationStrategy
 * Author       : dounguk
 * Date         : 2025. 10. 18.
 * Description  : SSE 알림 전송 전략 인터페이스
 *                Kafka -> Redis -> Memory 순으로 Fallback
 */
public interface SseNotificationStrategy {

    void publishNotification(UUID userId, NotificationDto notification);
}
