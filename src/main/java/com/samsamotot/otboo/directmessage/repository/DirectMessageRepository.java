package com.samsamotot.otboo.directmessage.repository;

import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.repository
 * FileName     : DirectMessageRepository
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {


    // 양방향, 우선순위: cursor, 타이브레이커 idAfter, DESC 고정
    @Query("""
            SELECT m FROM DirectMessage m
            JOIN FETCH m.sender s
            JOIN FETCH m.receiver r
            WHERE ((s.id = :me AND r.id = :other) OR (s.id = :other AND r.id = :me))
              AND (
                    :cursor IS NULL
                 OR m.createdAt < :cursor
                 OR (m.createdAt = :cursor AND (:idAfter IS NULL OR m.id < :idAfter))
              )
            ORDER BY m.createdAt DESC, m.id DESC
        """)
    List<DirectMessage> findBetweenWithCursor(
        @Param("me") UUID me,
        @Param("other") UUID other,
        @Param("cursor") Instant cursor,   // nullable
        @Param("idAfter") UUID idAfter,    // nullable
        Pageable pageable
    );

    @Query("""
            SELECT COUNT(m) FROM DirectMessage m
            WHERE ((m.sender.id = :me AND m.receiver.id = :other)
                OR (m.sender.id = :other AND m.receiver.id = :me))
        """)
    long countBetween(@Param("me") UUID me, @Param("other") UUID other);
}
