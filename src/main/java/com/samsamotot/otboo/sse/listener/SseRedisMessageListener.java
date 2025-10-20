package com.samsamotot.otboo.sse.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.listener
 * FileName     : SseRedisMessageListener
 * Author       : dounguk
 * Date         : 2025. 10. 16.
 * Description  : Redis Pub/Sub를 통한 SSE 메시지 수신 처리 (Kafka Fallback용)
 *                Kafka가 사용 불가능할 때 Redis를 통한 메시징 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseRedisMessageListener implements MessageListener {

    private static final String SSE_REDIS_LISTENER = "[SseRedisMessageListener] ";
    
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String messageStr = new String(message.getBody(), StandardCharsets.UTF_8);

            log.info(SSE_REDIS_LISTENER + "메시지 수신 - channel: {}, bytes: {}", channel, message.getBody().length);

            if (!channel.startsWith("sse:notification:")) return;

            UUID userId = UUID.fromString(channel.substring("sse:notification:".length()));

            JsonNode root = objectMapper.readTree(messageStr);

            final NotificationDto dto = root.isTextual()
                ? objectMapper.readValue(root.asText(), NotificationDto.class)
                : objectMapper.treeToValue(root, NotificationDto.class);

            sseService.sendLocalNotification(userId, objectMapper.writeValueAsString(dto));

            log.info(SSE_REDIS_LISTENER + "메시지 처리 완료 - userId: {}, notificationId: {}", userId, dto.getId());

        } catch (Exception e) {
            log.error(SSE_REDIS_LISTENER + "메시지 처리 실패 - channel: {}, bytes: {}",
                new String(message.getChannel(), StandardCharsets.UTF_8),
                message.getBody().length, e);
        }
    }
}
