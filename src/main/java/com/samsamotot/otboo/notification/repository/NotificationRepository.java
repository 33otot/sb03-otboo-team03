package com.samsamotot.otboo.notification.repository;

import com.samsamotot.otboo.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.repository
 * FileName     : NotificationRepository
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
