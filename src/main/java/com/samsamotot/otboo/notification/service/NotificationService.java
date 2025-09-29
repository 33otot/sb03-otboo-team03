package com.samsamotot.otboo.notification.service;

import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.service
 * FileName     : NotificationService
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Service
public interface NotificationService {
    Notification save(UUID receiverId, String title, String content, NotificationLevel level);

    void notifyRole(UUID userId);

    void notifyClothesAttrbute(UUID userId);

    void notifyLike(UUID commenterId, UUID feedOwnerId);

    void notifyComment(UUID commenterId, UUID feedOwnerId, String commentPreview);

    void notifyFollow(UUID followerId, UUID followeeId);

    void notifyDirectMessage(UUID senderId, UUID receiverId, String messagePreview);

}
