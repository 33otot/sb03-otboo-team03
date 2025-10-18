package com.samsamotot.otboo.sse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.sse.strategy.SseNotificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Deque;
import java.util.Map;
import java.util.Set;
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
    
    private static final String SSE_SERVICE = "[SseServiceImpl] ";
    private static final int MAX_BACKLOG = 1000;
    
    private final ObjectMapper objectMapper;
    private final SseNotificationStrategy sseNotificationStrategy;
    
    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    // 로컬 서버의 SSE 연결 관리 (사용자당 다중 연결 지원)
    private final Map<UUID, Set<SseEmitter>> connections = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<NotificationDto>> backlog = new ConcurrentHashMap<>();

    /**
     * 사용자에게 SSE 연결을 생성합니다.
     * 
     * @param userId 연결을 생성할 사용자 ID
     * @return 생성된 SseEmitter 인스턴스
     */
    @Override
    public SseEmitter createConnection(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.debug(SSE_SERVICE + "연결 종료 userId: {}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.debug(SSE_SERVICE + "타임아웃 userId: {}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onError(e -> {
            log.error(SSE_SERVICE + "연결 에러 for user: {}", userId, e);
            removeEmitter(userId, emitter);
        });

        connections.computeIfAbsent(userId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(emitter);
        int totalConnections = connections.values().stream().mapToInt(java.util.Set::size).sum();
        log.info(SSE_SERVICE + "생성 user: {}, 총 연결수: {}", userId, totalConnections);
        return emitter;
    }

    /**
     * 특정 사용자에게 알림을 전송합니다.
     * 로컬 연결이 있으면 직접 전송하고, 없으면 Redis Pub/Sub로 전송합니다.
     * 
     * @param userId 알림을 받을 사용자 ID
     * @param notificationData 알림 데이터 (JSON 문자열)
     */
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

        // 항상 메시징 전략을 통해 분산 메시징 수행
        try {
            NotificationDto notificationDto = objectMapper.readValue(notificationData, NotificationDto.class);
            sseNotificationStrategy.publishNotification(userId, notificationDto);
            log.info(SSE_SERVICE + "메시징 전략 발행 완료 - userId: {}", userId);
        } catch (Exception e) {
            log.error(SSE_SERVICE + "메시징 전략 발행 실패 - userId: {}", userId, e);
        }

        // 로컬 연결이 있으면 로컬로도 전송 (중복 전송 방지)
        Set<SseEmitter> emitters = connections.get(userId);
        if (emitters != null && !emitters.isEmpty()) {
            for (SseEmitter em : emitters.toArray(new SseEmitter[0])) { // 방어적 복사
                try {
                    em.send(SseEmitter.event()
                        .name("notifications")
                        .id(dto.getId().toString())
                        .data(notificationData));
                    log.info(SSE_SERVICE + "로컬 알림 전송 완료 - user: {}", userId);
                } catch (Exception e) {
                    log.error(SSE_SERVICE + "로컬 알림 전송실패 user: {}", userId, e);
                    removeEmitter(userId, em);
                }
            }
        }
    }

    /**
     * 사용자가 놓친 이벤트들을 재전송합니다.
     * 
     * @param userId 사용자 ID
     * @param lastEventId 마지막으로 받은 이벤트 ID (null이면 아무것도 전송하지 않음)
     * @param emitter SSE 연결 객체
     */
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

    /**
     * 현재 활성화된 SSE 연결 수를 반환합니다.
     * 
     * @return 활성 연결 수
     */
    @Override
    public int getActiveConnectionCount() {
        return connections.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * 특정 사용자가 현재 연결되어 있는지 확인합니다.
     * 
     * @param userId 확인할 사용자 ID
     * @return 연결 여부
     */
    @Override
    public boolean isUserConnected(UUID userId) {
        java.util.Set<SseEmitter> set = connections.get(userId);
        return set != null && !set.isEmpty();
    }

    /**
     * Redis에서 받은 메시지를 로컬 SSE 연결로만 전송 (재발행 방지)
     * 
     * @param userId 알림을 받을 사용자 ID
     * @param notificationData 알림 데이터 (JSON 문자열)
     */
    public void sendLocalNotification(UUID userId, String notificationData) {
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

        // 로컬 연결 확인 및 전송
        Set<SseEmitter> emitters = connections.get(userId);
        if (emitters != null && !emitters.isEmpty()) {
            for (SseEmitter em : emitters.toArray(new SseEmitter[0])) { // 방어적 복사
                try {
                    em.send(SseEmitter.event()
                        .name("notifications")
                        .id(dto.getId().toString())
                        .data(notificationData));
                } catch (Exception e) {
                    log.error(SSE_SERVICE + "로컬 알림 전송실패 user: {}", userId, e);
                    removeEmitter(userId, em);
                }
            }
            log.info(SSE_SERVICE + "로컬 알림 전송 완료 user: {}", userId);
        } else {
            log.info(SSE_SERVICE + "로컬 연결 없음 - 알림 건너뜀 user: {}", userId);
        }
    }

    /**
     * 사용자별 Emitter 제거 헬퍼 메서드
     * 
     * @param userId 사용자 ID
     * @param emitter 제거할 Emitter
     */
    private void removeEmitter(UUID userId, SseEmitter emitter) {
        connections.computeIfPresent(userId, (k, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
    }
}