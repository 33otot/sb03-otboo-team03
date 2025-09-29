package com.samsamotot.otboo.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.samsamotot.otboo.notification.dto.NotificationPayload;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.manager.NotificationCursor;
import com.samsamotot.otboo.notification.repository.NotificationRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * PackageName  : com.samsamotot.otboo.notification.service
 * FileName     : NotificationServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Transactional
    public Notification saveAndPublish(UUID receiverId, String title, String content, NotificationLevel level) {
        User receiverRef = userRepository.getReferenceById(receiverId);

        Notification notification = notificationRepository.save(
            Notification.builder()
                .receiver(receiverRef)
                .title(title)
                .content(content)
                .level(level)
                .build()
        );

        publishJson(notification);
        return notification;
    }

    /**
     * 이미 저장된 알림을 다시 발행하고 싶을 때 사용
     */
    @Transactional(readOnly = true)
    public void publishJson(Notification notification) {
        try {
            String cursor = notification.getCreatedAt().toEpochMilli() + ":" + notification.getId();

            ObjectNode root = objectMapper.createObjectNode();
            root.put("id", notification.getId().toString());
            root.put("receiverId", notification.getReceiver().getId().toString());
            root.put("title", notification.getTitle());
            root.put("content", notification.getContent());
            root.put("level", notification.getLevel().name());
            root.put("createdAt", notification.getCreatedAt().toString());
            root.put("cursor", cursor);

            String json = objectMapper.writeValueAsString(root);
            redis.convertAndSend("notify:" + notification.getReceiver().getId(), json);
        } catch (Exception ignore) {
            // 로깅만 (DB 리플레이로 복구 가능)
        }
    }
}

