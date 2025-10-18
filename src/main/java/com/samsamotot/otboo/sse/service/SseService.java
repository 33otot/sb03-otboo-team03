package com.samsamotot.otboo.sse.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.service
 * FileName     : SseService
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */
public interface SseService {
    SseEmitter createConnection(UUID userId);

    void sendNotification(UUID userId, String notificationData);

    void sendLocalNotification(UUID userId, String notificationData);

    void replayMissedEvents(UUID userId, String lastEventId, SseEmitter emitter);

    int getActiveConnectionCount();

    boolean isUserConnected(UUID userId);
}
