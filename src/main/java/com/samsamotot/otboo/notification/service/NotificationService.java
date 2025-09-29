package com.samsamotot.otboo.notification.service;

import com.samsamotot.otboo.notification.dto.NotificationPayload;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * PackageName  : com.samsamotot.otboo.notification.service
 * FileName     : NotificationService
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Service
public interface NotificationService {
    Notification saveAndPublish(UUID receiverId, String title, String content, NotificationLevel level);

    void publishJson(Notification n);
}3