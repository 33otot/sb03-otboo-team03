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
                :ts is null
             or n.createdAt > :ts
             or (n.createdAt = :ts and (:afterId is null or n.id > :afterId))
          )
        order by n.createdAt asc, n.id asc
    """)
    List<Notification> findAllAfter(
        @Param("userId") UUID userId,
        @Param("ts") Instant ts,
        @Param("afterId") UUID afterId
    );
}