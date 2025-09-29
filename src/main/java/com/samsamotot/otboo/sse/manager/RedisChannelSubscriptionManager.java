package com.samsamotot.otboo.sse.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationPayload;
import com.samsamotot.otboo.notification.manager.NotificationCursor;
import com.samsamotot.otboo.sse.registry.SseEmitterRegistry;
import com.samsamotot.otboo.sse.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PackageName  : com.samsamotot.otboo.notification.manager
 * FileName     : RedisChannelSubscriptionManager
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Component
@RequiredArgsConstructor
public class RedisChannelSubscriptionManager {

    private final RedisMessageListenerContainer container;
    private final ObjectMapper om;
    private final SseEmitterRegistry emitters;

    private final Map<UUID, MessageListener> listeners = new ConcurrentHashMap<>();

    public synchronized void subscribe(UUID userId) {
        if (listeners.containsKey(userId)) return;

        String topic = topic(userId);
        MessageListener l = (message, pattern) -> {
            try {
                var payload = om.readValue(message.getBody(), NotificationPayload.class);
                emitters.get(userId).ifPresent(em -> send(em, payload));
            } catch (Exception ignore) {}
        };
        container.addMessageListener(l, new ChannelTopic(topic));
        listeners.put(userId, l);
    }

    public synchronized void unsubscribe(UUID userId) {
        var l = listeners.remove(userId);
        if (l != null) container.removeMessageListener(l, new ChannelTopic(topic(userId)));
    }

    private static String topic(UUID userId) { return "notify:" + userId; }

    private void send(SseEmitter em, NotificationPayload p) {
        try {
            String id = NotificationCursor.encode(p.createdAt(), p.id());
            em.send(SseEmitter.event().id(id).name("notify").data(p));
        } catch (Exception e) {
            em.completeWithError(e);
        }
    }
}