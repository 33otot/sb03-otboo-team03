package com.samsamotot.otboo.sse.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PackageName  : com.samsamotot.otboo.sse.transport
 * FileName     : MemorySseNotificationService
 * Author       : dounguk
 * Date         : 2025. 10. 18.
 * Description  : 메모리 기반 SSE 알림 전송 서비스 (최후의 Fallback용)
 *                단일 인스턴스 내에서만 동작
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemorySseNotificationTransport {

    private static final String MEMORY_SSE_NOTIFICATION_TRANSPORT = "[MemorySseNotificationTransport] ";

    // 사용자별 메시지 큐 (메모리 저장)
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<String>> userMessageQueues = new ConcurrentHashMap<>();

    /**
     * 메모리를 통한 SSE 알림 발행
     */
    public void publishNotification(UUID userId, String notificationData) {
        try {
            userMessageQueues.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(notificationData);
            
            log.debug(MEMORY_SSE_NOTIFICATION_TRANSPORT + "Memory 발행 성공 - userId: {}, queueSize: {}", userId, getUserQueueSize(userId));
        } catch (Exception e) {
            log.error(MEMORY_SSE_NOTIFICATION_TRANSPORT + "Memory 발행 실패 - userId: {}", userId, e);
            throw e;
        }
    }

    /**
     * 사용자의 메시지 큐 크기 반환
     */
    public int getUserQueueSize(UUID userId) {
        return userMessageQueues.getOrDefault(userId, new CopyOnWriteArrayList<>()).size();
    }
}
