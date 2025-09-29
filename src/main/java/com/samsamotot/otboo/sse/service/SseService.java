package com.samsamotot.otboo.sse.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.service
 * FileName     : SseService
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */
@Service
public interface SseService {
    SseEmitter createConnection(UUID userId);

    void sendNotification(UUID userId, String notificationData);

    int getConnectionCount();

    boolean isUserConnected(UUID userId);

    void closeAllConnections();
}
