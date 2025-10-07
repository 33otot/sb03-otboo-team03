package com.samsamotot.otboo.notification.service;

import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import com.samsamotot.otboo.notification.dto.NotificationRequest;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.service
 * FileName     : NotificationService
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Validated
@Service
public interface NotificationService {
    Notification save(UUID receiverId, String title, String content, NotificationLevel level);

    void notifyRole(UUID userId);

    void notifyClothesAttribute();

    void notifyLike(UUID commenterId, UUID feedId);

    void notifyComment(UUID commenterId, UUID feedId, String comment);

    void notifyFollow(UUID followerId, UUID followeeId);

    void notifyDirectMessage(UUID senderId, UUID receiverId, String messagePreview);

    NotificationListResponse getNotifications(@Valid NotificationRequest request);

    void delete(UUID notificationId);
}
