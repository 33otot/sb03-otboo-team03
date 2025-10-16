package com.samsamotot.otboo.sse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PackageName  : com.samsamotot.otboo.sse.service
 * FileName     : SseServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 * Description  : Redis를 활용한 분산 SSE 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseServiceImpl implements SseService {
    
    private static final String SSE_SERVICE = "[SSE_SERVICE]";
    private static final int MAX_BACKLOG = 1000;
    
    private final ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // 로컬 서버의 SSE 연결 관리
    private final Map<UUID, SseEmitter> connections = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<NotificationDto>> backlog = new ConcurrentHashMap<>();

    @Override
    public SseEmitter createConnection(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.debug(SSE_SERVICE + "연결 성공 후 종료 userId: {}", userId);
            connections.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.debug(SSE_SERVICE + "타임아웃 userId: {}", userId);
            connections.remove(userId);
        });

        emitter.onError(e -> {
            log.error(SSE_SERVICE + "연결 에러 for user: {}", userId, e);
            connections.remove(userId);
        });

        connections.put(userId, emitter);
        log.info(SSE_SERVICE + "생성 user: {}, 총: {}", userId, connections.size());
        return emitter;
    }

    @Override
    public void sendNotification(UUID userId, String notificationData) {
        final NotificationDto dto;
        try {
            dto = objectMapper.readValue(notificationData, NotificationDto.class);
        } catch (Exception e) {
            log.error(SSE_SERVICE + "잘못된 JSON, user={}", userId, e);
            return;
        }

        // 백로그에 추가
        Deque<NotificationDto> q = backlog.computeIfAbsent(userId, k -> new java.util.concurrent.ConcurrentLinkedDeque<>());
        q.addLast(dto);
        while (q.size() > MAX_BACKLOG) q.pollFirst();

        // 로컬 연결 확인
        SseEmitter emitter = connections.get(userId);
        if (emitter != null) {
            // 로컬 연결이 있으면 직접 전송
            try {
                emitter.send(SseEmitter.event()
                    .name("notifications")
                    .id(dto.getId().toString())
                    .data(notificationData));
                log.debug(SSE_SERVICE + "로컬 알림 전송 user: {}", userId);
                return;
            } catch (IOException e) {
                log.error(SSE_SERVICE + "로컬 알림 전송실패 user: {}", userId, e);
                connections.compute(userId, (k, v) -> v == emitter ? null : v);
            }
        }

        // 로컬 연결이 없으면 Redis Pub/Sub로 다른 서버에 전송 요청
        publishNotification2(userId, notificationData);
    }

    @Override
    public void replayMissedEvents(UUID userId, String lastEventId, SseEmitter emitter) {
        Deque<NotificationDto> q = backlog.get(userId);
        if (q == null || q.isEmpty()) return;

        // lastEventId가 null이면 아무것도 전송하지 않음
        if (lastEventId == null || lastEventId.isBlank()) {
            return;
        }

        boolean found = false;
        for (NotificationDto dto : q) {
            if (found) {
                try {
                    String json = objectMapper.writeValueAsString(dto);
                    emitter.send(SseEmitter.event()
                        .name("notifications")
                        .id(dto.getId().toString())
                        .data(json));
                } catch (Exception e) {
                    log.error(SSE_SERVICE + "리플레이 실패 user: {}, notificationId: {}", userId, dto.getId(), e);
                    // IOException 발생 시 중단
                    break;
                }
            } else if (dto.getId().toString().equals(lastEventId)) {
                found = true;
            }
        }
    }

    @Override
    public int getActiveConnectionCount() {
        return connections.size();
    }

    @Override
    public boolean isUserConnected(UUID userId) {
        return connections.containsKey(userId);
    }

    @Override
    public void sendBatchNotification(java.util.List<UUID> userIds, String notificationData) {
        try {
            // JSON 유효성 검사
            objectMapper.readValue(notificationData, NotificationDto.class);
        } catch (Exception e) {
            log.error(SSE_SERVICE + "잘못된 JSON for batch notification", e);
            return;
        }

        java.util.List<UUID> localUsers = new java.util.ArrayList<>();
        java.util.List<UUID> remoteUsers = new java.util.ArrayList<>();

        // 사용자를 로컬/원격으로 분류
        for (UUID userId : userIds) {
            if (connections.containsKey(userId)) {
                localUsers.add(userId);
            } else {
                remoteUsers.add(userId);
            }
        }

        // 로컬 사용자들에게 직접 전송
        for (UUID userId : localUsers) {
            sendNotification(userId, notificationData);
        }

        // 원격 사용자들에게는 Redis Pub/Sub로 전송
        if (!remoteUsers.isEmpty()) {
            publishBatchNotification2(remoteUsers, notificationData);
        }

        log.info(SSE_SERVICE + "배치 알림 전송 완료 - 로컬: {}, 원격: {}", 
                localUsers.size(), remoteUsers.size());
    }

    /**
     * 특정 사용자에게 알림을 Redis Pub/Sub로 발행
     * 
     * @param userId 알림을 받을 사용자 ID
     * @param notificationData 알림 데이터 (JSON 문자열)
     */
    private void publishNotification2(UUID userId, String notificationData) {
        if (redisTemplate == null) {
            log.debug(SSE_SERVICE + "Redis 비활성화 - 알림 발행 건너뜀 userId: {}", userId);
            return;
        }
        
        try {
            String channel = "sse:notification:" + userId;
            redisTemplate.convertAndSend(channel, notificationData);
            log.debug(SSE_SERVICE + "알림 발행 - userId: {}, channel: {}", userId, channel);
        } catch (Exception e) {
            log.error(SSE_SERVICE + "알림 발행 실패 - userId: {}", userId, e);
        }
    }

    /**
     * 여러 사용자에게 동일한 알림을 배치로 발행
     * 
     * @param userIds 알림을 받을 사용자 ID 목록
     * @param notificationData 알림 데이터 (JSON 문자열)
     */
    private void publishBatchNotification2(java.util.List<UUID> userIds, String notificationData) {
        for (UUID userId : userIds) {
            publishNotification2(userId, notificationData);
        }
        log.info(SSE_SERVICE + "배치 알림 발행 완료 - 대상 사용자 수: {}", userIds.size());
    }
}
