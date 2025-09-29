package com.samsamotot.otboo.sse.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PackageName  : com.samsamotot.otboo.notification.repository
 * FileName     : EmitterRepository
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Component
public class EmitterRepository {
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(UUID userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));
        return emitter;
    }

    public Optional<SseEmitter> get(UUID userId) {
        return Optional.ofNullable(emitters.get(userId));
    }

    public void remove(UUID userId) { emitters.remove(userId); }
}
