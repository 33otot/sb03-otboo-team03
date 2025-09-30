package com.samsamotot.otboo.sse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * PackageName  : com.samsamotot.otboo.sse.service
 * FileName     : SseServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseServiceImpl implements SseService {
    private static final String SSE_SERVICE = "[SSE_SERVICE]";
    private final ObjectMapper objectMapper;

    private final Map<UUID, SseEmitter> connections = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<NotificationDto>> backlog = new ConcurrentHashMap<>();
    private static final int MAX_BACKLOG = 1000;

    /**
     * 특정 사용자와 SSE 연결을 생성하고 관리 맵에 등록한다.
     * 연결 종료, 타임아웃, 에러 발생 시 자동으로 맵에서 제거된다.
     *
     * @param userId SSE 연결을 요청한 사용자 ID
     * @return 생성된 SseEmitter 객체
     */
    public SseEmitter createConnection(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.debug(SSE_SERVICE + " 연결 성공 userId: {}", userId);
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

    /**
     * 특정 사용자에게 알림 데이터를 SSE 이벤트로 전송한다.
     * 연결이 없거나 전송 실패 시 해당 연결을 제거한다.
     *
     * @param userId 알림을 받을 사용자 ID
     * @param notificationData 전송할 알림 데이터 (문자열)
     */
    public void sendNotification(UUID userId, String notificationData) {
        final NotificationDto dto;
        try {
            dto = objectMapper.readValue(notificationData, NotificationDto.class);
        } catch (Exception e) {
            log.error(SSE_SERVICE + "잘못된 JSON, user={}", userId, e);
            return;
        }

        Deque<NotificationDto> q = backlog.computeIfAbsent(userId, k -> new java.util.concurrent.ConcurrentLinkedDeque<>());
        q.addLast(dto);
        while (q.size() > MAX_BACKLOG) q.pollFirst();

        SseEmitter emitter = connections.get(userId);
        if (emitter == null) {
            log.debug(SSE_SERVICE + "연결 없음 user: {}", userId);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .name("notifications")
                .id(dto.getId().toString())
                .data(notificationData));
            log.debug(SSE_SERVICE + "알람 전송 user: {}", userId);
        } catch (IOException e) {
            log.error(SSE_SERVICE + "알람 전송실패 user: {}", userId, e);
            connections.compute(userId, (k, v) -> v == emitter ? null : v);
        }
    }

    /**
     * 재연결 시 누락 이벤트 리플레이.
     * lastEventId 이후의 이벤트만 JSON 문자열로 동일 포맷 전송.
     */
    public void replayMissedEvents(UUID userId, String lastEventId, SseEmitter emitter) {
        if (lastEventId == null || lastEventId.isBlank()) return;
        Deque<NotificationDto> q = backlog.get(userId);
        if (q == null || q.isEmpty()) return;

        boolean after = false;
        for (NotificationDto dto : q) {
            if (!after) {
                after = dto.getId().toString().equals(lastEventId);
                continue;
            }
            try {
                emitter.send(SseEmitter.event()
                    .name("notifications")
                    .id(dto.getId().toString())
                    .data(objectMapper.writeValueAsString(dto)));
            } catch (IOException e) {
                log.warn(SSE_SERVICE + "리플레이 중단 user={}", userId, e);
                break;
            }
        }
    }

}
