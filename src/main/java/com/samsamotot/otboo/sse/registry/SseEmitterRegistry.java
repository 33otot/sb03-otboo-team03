package com.samsamotot.otboo.sse.registry;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PackageName  : com.samsamotot.otboo.sse.registry
 * FileName     : SsmEmitterRegistry
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */

@Component
public class SseEmitterRegistry {
    private final Map<UUID, SseEmitter> map = new ConcurrentHashMap<>();

    public SseEmitter add(UUID userId, SseEmitter emitter) {
        map.put(userId, emitter);
        emitter.onCompletion(() -> map.remove(userId));
        emitter.onTimeout(() -> map.remove(userId));
        emitter.onError(e -> map.remove(userId));
        return emitter;
    }

    public Optional<SseEmitter> get(UUID userId) {
        return Optional.ofNullable(map.get(userId));
    }
}
