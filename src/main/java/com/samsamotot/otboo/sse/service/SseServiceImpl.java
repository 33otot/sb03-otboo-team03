package com.samsamotot.otboo.sse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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

    private final Map<UUID, SseEmitter> connections = new ConcurrentHashMap<>();

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
        SseEmitter emitter = connections.get(userId);
        if (emitter != null) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name("notifications")
                    .data(notificationData)
                    .id(UUID.randomUUID().toString());

                emitter.send(event);
                log.debug(SSE_SERVICE + "알람 전송 user: {}", userId);

            } catch (IOException e) {
                log.error(SSE_SERVICE +"알람 전송실패 user: {}, removing connection", userId, e);
                connections.remove(userId);
            }
        } else {
            log.debug(SSE_SERVICE + "연결 없음 user: {}", userId);
        }
    }
}
