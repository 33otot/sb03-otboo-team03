package com.samsamotot.otboo.sse.listener;

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
 * Description  : Redis Pub/Sub를 통한 SSE 메시지 수신 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseRedisMessageListener implements MessageListener {

    private static final String SSE_REDIS_LISTENER = "[SSE_REDIS_LISTENER] ";
    
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String messageStr = new String(message.getBody(), StandardCharsets.UTF_8);
            
            log.debug(SSE_REDIS_LISTENER + "메시지 수신 - channel: {}, bytes: {}", channel, message.getBody().length);
            
            // 채널에서 userId 추출 (sse:notification:userId)
            if (channel.startsWith("sse:notification:")) {
                String userIdStr = channel.substring("sse:notification:".length());
                UUID userId = UUID.fromString(userIdStr);
                
                // 메시지를 NotificationDto로 파싱
                NotificationDto notificationDto = objectMapper.readValue(messageStr, NotificationDto.class);
                
                // 로컬 SSE 연결로만 메시지 전송 (재발행 방지)
                sseService.sendLocalNotification(userId, messageStr);
                
                log.debug(SSE_REDIS_LISTENER + "메시지 처리 완료 - userId: {}, notificationId: {}", 
                         userId, notificationDto.getId());
            }
                     
        } catch (Exception e) {
            log.error(SSE_REDIS_LISTENER + "메시지 처리 실패 - channel: {}, bytes: {}", 
                     new String(message.getChannel(), StandardCharsets.UTF_8), message.getBody().length, e);
        }
    }
}
