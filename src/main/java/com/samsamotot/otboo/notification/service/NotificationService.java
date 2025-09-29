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

    void notifyDirectMessage(UUID senderId, UUID receiverId, String messagePreview);

    void notifyFollow(UUID followerId, UUID followeeId);

    void notifyComment(UUID commenterId, UUID feedOwnerId, String commentPreview);
}
