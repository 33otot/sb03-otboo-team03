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


    public int getConnectionCount() {
        return connections.size();
    }

    public boolean isUserConnected(UUID userId) {
        return connections.containsKey(userId);
    }

    public void closeAllConnections() {
        connections.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error(SSE_SERVICE+"전체 연결 닫는중 에러", e);
            }
        });
        connections.clear();
    }
}
