package com.samsamotot.otboo.notification.repository;

import com.samsamotot.otboo.notification.entity.Notification;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.repository
 * FileName     : NotificationRepository
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
        select n from Notification n
        where n.receiver.id = :userId
          and (
                :timestamp is null
             or n.createdAt < :timestamp
             or (n.createdAt = :timestamp and (:beforeId is null or n.id < :beforeId))
          )
        order by n.createdAt desc, n.id desc
    """)
    List<Notification> findAllBefore(
        @Param("userId") UUID userId,
        @Param("timestamp") Instant timestamp,
        @Param("beforeId") UUID beforeId,
        org.springframework.data.domain.Pageable pageable
    );

    long countByReceiver_Id(UUID userId);
}