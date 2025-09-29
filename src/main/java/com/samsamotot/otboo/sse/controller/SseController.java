package com.samsamotot.otboo.sse.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.repository.NotificationRepository;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.sse.manager.RedisChannelSubscriptionManager;
import com.samsamotot.otboo.sse.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PackageName  : com.samsamotot.otboo.sse.controller
 * FileName     : SseController
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@RestController
@RequestMapping("/api")
public class SseController {
    private static final long TIMEOUT = 60L * 60 * 1000L;

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer redisContainer;

    // 로컬 연결 및 리스너 관리
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<UUID, MessageListener> listeners = new ConcurrentHashMap<>();

    public SseController(
        NotificationRepository notificationRepository,
        ObjectMapper objectMapper,
        RedisConnectionFactory redisConnectionFactory
    ) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;

        this.redisContainer = new RedisMessageListenerContainer();
        this.redisContainer.setConnectionFactory(redisConnectionFactory);
        this.redisContainer.afterPropertiesSet();
        this.redisContainer.start();
    }

    /**
     * SSE 구독 엔드포인트
     * @param userId            (필수) 구독 대상 사용자
     * @param lastEventIdParam  (선택) Swagger 수동 테스트용 UUID
     * @param lastEventIdHeader (선택) 브라우저 자동 재연결 헤더 "epochMillis:uuid"
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
        @RequestParam("userId") UUID userId,
        @RequestParam(value = "LastEventId", required = false) UUID lastEventIdParam,
        @RequestHeader(value = "Last-Event-ID", required = false) String lastEventIdHeader
    ) {
        SseEmitter em = new SseEmitter(TIMEOUT);
        emitters.put(userId, em);

        // 1) 연결 확인
        try { em.send(SseEmitter.event().name("connect").data("ok")); } catch (Exception ignore) {}

        // 2) Redis 채널 동적 구독 (notify:{userId})
        subscribe(userId);

        // 3) 미수신 리플레이
        replayMissed(userId, lastEventIdHeader, lastEventIdParam, em);

        // 4) 정리 핸들러
        em.onCompletion(() -> cleanup(userId));
        em.onTimeout(() -> cleanup(userId));
        em.onError(e -> cleanup(userId));
        return em;
    }

    private void subscribe(UUID userId) {
        if (listeners.containsKey(userId)) return;

        String topic = "notify:" + userId;
        MessageListener l = (msg, pattern) -> {
            SseEmitter em = emitters.get(userId);
            if (em == null) return;

            String json = new String(msg.getBody(), StandardCharsets.UTF_8);
            String cursor = extractCursor(json); // "epochMillis:uuid" (퍼블리셔가 포함)

            try {
                var ev = SseEmitter.event().name("notify").data(json);
                if (cursor != null) ev.id(cursor);
                em.send(ev);
            } catch (Exception e) {
                em.completeWithError(e);
            }
        };

        redisContainer.addMessageListener(l, new ChannelTopic(topic));
        listeners.put(userId, l);
    }

    private void replayMissed(UUID userId, String lastEventIdHeader, UUID lastEventIdParam, SseEmitter em) {
        Instant ts = null; UUID afterId = null;

        // A) 브라우저 자동 헤더 "millis:uuid"
        if (lastEventIdHeader != null && !lastEventIdHeader.isBlank()) {
            String[] p = lastEventIdHeader.split(":", 2);
            if (p.length == 2) {
                try {
                    ts = Instant.ofEpochMilli(Long.parseLong(p[0]));
                    afterId = UUID.fromString(p[1]);
                } catch (Exception ignore) {}
            }
        }

        // B) Swagger 쿼리(UUID) → createdAt/UUID로 보강
        if (ts == null && lastEventIdParam != null) {
            var pivot = notificationRepository.findById(lastEventIdParam).orElse(null);
            if (pivot != null) {
                ts = pivot.getCreatedAt();
                afterId = pivot.getId();
            }
        }

        if (ts == null) return;

        var list = notificationRepository.findAllAfter(userId, ts, afterId);
        for (Notification n : list) {
            String cursor = encodeCursor(n.getCreatedAt(), n.getId());
            try {
                em.send(SseEmitter.event()
                    .id(cursor)
                    .name("notify")
                    .data(toJson(n)));
            } catch (Exception e) {
                em.completeWithError(e);
                return;
            }
        }
    }

    private void cleanup(UUID userId) {
        emitters.remove(userId);
        var l = listeners.remove(userId);
        if (l != null) {
            redisContainer.removeMessageListener(l, new ChannelTopic("notify:" + userId));
        }
    }

    // ---------- JSON / 커서 유틸 ----------

    private String extractCursor(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode c = node.get("cursor");
            return (c != null && c.isTextual()) ? c.asText() : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private String encodeCursor(Instant createdAt, UUID id) {
        return createdAt.toEpochMilli() + ":" + id;
    }

    private String toJson(Notification n) {
        try {
            var root = objectMapper.createObjectNode();
            root.put("id", n.getId().toString());
            root.put("receiverId", n.getReceiver().getId().toString());
            root.put("title", n.getTitle());
            root.put("content", n.getContent());
            root.put("level", n.getLevel().name());
            root.put("createdAt", n.getCreatedAt().toString());
            root.put("cursor", encodeCursor(n.getCreatedAt(), n.getId()));
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"id\":\"" + n.getId() + "\"}";
        }
    }
}
