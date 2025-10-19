package com.samsamotot.otboo.sse.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.listener
 * FileName     : SseKafkaMessageListener
 * Author       : dounguk
 * Date         : 2025. 10. 17.
 * Description  : Kafka를 통한 SSE 메시지 수신 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseKafkaMessageListener {

    private static final String SSE_KAFKA_LISTENER = "[SseKafkaMessageListener] ";
    
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    /**
     * Kafka에서 SSE 알림 메시지를 수신하여 로컬 SSE 연결로 전송
     * 
     * @param userId 알림을 받을 사용자 ID (파티션 키)
     * @param notificationData 알림 데이터 (JSON 문자열)
     * @param partition 파티션 번호
     * @param offset 오프셋
     */
    @KafkaListener(
        topics = "sse-notifications",
        groupId = "otboo-sse-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSseNotification(
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            @Payload String notificationData,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        try {
            log.info(SSE_KAFKA_LISTENER + "메시지 수신 - userId: {}, partition: {}, offset: {}, bytes: {}", 
                     userId, partition, offset, notificationData.length());
            
            NotificationDto notificationDto = objectMapper.readValue(notificationData, NotificationDto.class);
            
            UUID userIdUuid = UUID.fromString(userId);
            
            sseService.sendLocalNotification(userIdUuid, notificationData);
            
            log.info(SSE_KAFKA_LISTENER + "메시지 처리 완료 - userId: {}, notificationId: {}, partition: {}, offset: {}", 
                     userId, notificationDto.getId(), partition, offset);
                     
        } catch (Exception e) {
            log.error(SSE_KAFKA_LISTENER + "메시지 처리 실패 - userId: {}, partition: {}, offset: {}, data: {}", 
                     userId, partition, offset, notificationData, e);
        }
    }
}
