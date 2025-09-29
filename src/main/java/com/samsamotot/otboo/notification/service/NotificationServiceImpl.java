package com.samsamotot.otboo.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.repository.NotificationRepository;
import com.samsamotot.otboo.sse.service.SseService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.service
 * FileName     : NotificationServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private static final String NOTIFICATION_SERVICE = "[NotificationService] ";

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    public Notification save(UUID receiverId, String title, String content, NotificationLevel level) {

        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> {
                log.warn(NOTIFICATION_SERVICE + "인증 사용자 조회 실패: receiverId={} (DB에 없음)", receiverId);
                return new OtbooException(ErrorCode.UNAUTHORIZED);
            });

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title(title)
            .content(content)
            .level(level)
            .build();

        Notification saved = repository.save(notification);

        // SSE로 실시간 발행
        try {
            String notificationJson = objectMapper.writeValueAsString(toDto(saved));
            sseService.sendNotification(receiverId, notificationJson);
            log.info(NOTIFICATION_SERVICE+" SSE 보냄 user: {}, title: '{}'", receiverId, title);
        } catch (Exception e) {
            log.error("[Notification] SSE 발행 실패해도 알림 저장은 성공했으므로 계속 진행 user: {}", receiverId, e);
        }

        return saved;
    }

    // 다른 비즈니스 로직에서의 알람
    public void notifyDirectMessage(UUID senderId, UUID receiverId, String messagePreview) {
        save(receiverId, "새 쪽지", "from: " + senderId + ", content: " + messagePreview, NotificationLevel.INFO);
    }

    public void notifyFollow(UUID followerId, UUID followeeId) {
        save(followeeId, "새 팔로워", "사용자가 팔로우했습니다", NotificationLevel.INFO);
    }

    public void notifyComment(UUID commenterId, UUID feedOwnerId, String commentPreview) {
        save(feedOwnerId, "새 댓글", "from: " + commenterId + ", content: " + commentPreview, NotificationLevel.INFO);
    }

    private NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
            .id(notification.getId())
            .createdAt(notification.getCreatedAt())
            .receiverId(notification.getReceiver().getId())
            .title(notification.getTitle())
            .content(notification.getContent())
            .level(notification.getLevel())
            .build();
    }
}