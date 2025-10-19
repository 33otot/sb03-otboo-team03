package com.samsamotot.otboo.notification.repository;

import com.samsamotot.otboo.notification.entity.Notification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

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
          SELECT n FROM Notification n
          JOIN FETCH n.receiver r
          WHERE r.id = :receiverId
          ORDER BY n.createdAt DESC, n.id DESC
        """)
    List<Notification> findLatest(UUID receiverId, Pageable pageable);

    @Query("""
           SELECT n FROM Notification n
           JOIN FETCH n.receiver r
           WHERE r.id = :receiverId
             AND (
                 n.createdAt < :cursor
                 OR (n.createdAt = :cursor AND n.id < :idAfter)
             )
           ORDER BY n.createdAt DESC, n.id DESC
        """)
    List<Notification> findAllBefore(
        @Param("receiverId") UUID receiverId,
        @Param("cursor") Instant cursor,
        @Param("idAfter") UUID idAfter,
        Pageable pageable
    );

    long countByReceiver_Id(UUID userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.receiver.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}