package com.samsamotot.otboo.directmessage.repository;

import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import org.springframework.data.jpa.repository.Modifying;
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


    /*
        목록조회 커서 X
        양방향, 우선순위: cursor, 타이브레이커 idAfter, DESC 고정
     */
    @Query("""
        SELECT m FROM DirectMessage m
        JOIN FETCH m.sender s
        JOIN FETCH m.receiver r
        WHERE ((s.id = :me AND r.id = :other) OR (s.id = :other AND r.id = :me))
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    List<DirectMessage> findFirstPage(
        @Param("me") UUID me,
        @Param("other") UUID other,
        Pageable pageable);


    /*
        목록조회 커서 O
        양방향, 우선순위: cursor, 타이브레이커 idAfter, DESC 고정
     */
    @Query("""
          SELECT m FROM DirectMessage m
          JOIN FETCH m.sender s
          JOIN FETCH m.receiver r
          WHERE ((s.id = :me AND r.id = :other) OR (s.id = :other AND r.id = :me))
            AND ( m.createdAt < :cursor
               OR (m.createdAt = :cursor AND (:idAfter IS NULL OR m.id < :idAfter))
            )
          ORDER BY m.createdAt DESC, m.id DESC
        """)
    List<DirectMessage> findNextPage(
        @Param("me") UUID me,
        @Param("other") UUID other,
        @Param("cursor") Instant cursor,
        @Param("idAfter") UUID idAfter,
        Pageable pageable);


    /*
        목록조회 카운트
     */
    @Query("""
            SELECT COUNT(m) FROM DirectMessage m
            WHERE ((m.sender.id = :me AND m.receiver.id = :other)
                OR (m.sender.id = :other AND m.receiver.id = :me))
        """)
    long countBetween(@Param("me") UUID me, @Param("other") UUID other);

    @Query(value = """
        SELECT COUNT(DISTINCT (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id)))
        FROM direct_messages
        WHERE sender_id = :userId OR receiver_id = :userId
        """, nativeQuery = true)
    long countConversations(@Param("userId") UUID userId);

    /**
     * 1. 각 대화방의 마지막 메시지 ID를 찾는 네이티브 쿼리.
     * ROW_NUMBER()를 사용하여 동률 문제를 해결하고, 가장 효율적으로 ID만 선택합니다.
     */
//    @Query(value = """
//        SELECT DISTINCT ON (
//          LEAST(sender_id, receiver_id),
//          GREATEST(sender_id, receiver_id)
//        ) id
//        FROM direct_messages
//        WHERE sender_id = :userId OR receiver_id = :userId
//        ORDER BY
//          LEAST(sender_id, receiver_id),
//          GREATEST(sender_id, receiver_id),
//          created_at DESC,
//          id DESC
//        """, nativeQuery = true)
//    List<UUID> findLastMessageIdsOfConversations(@Param("userId") UUID userId);

    @Query(value = """
        SELECT id
        FROM (
            SELECT
                id,
                created_at,
                ROW_NUMBER() OVER (PARTITION BY LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id) ORDER BY created_at DESC, id DESC) as rn
            FROM
                direct_messages
            WHERE
                (sender_id = :userId OR receiver_id = :userId)
        ) AS sub
        WHERE rn = 1
        ORDER BY created_at DESC, id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<UUID> findLastMessageIdsOfConversationsFirstPage(@Param("userId") UUID userId, @Param("limit") int limit);

    @Query(value = """
        SELECT id
        FROM (
            SELECT
                id,
                created_at,
                ROW_NUMBER() OVER (PARTITION BY LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id) ORDER BY created_at DESC, id DESC) as rn
            FROM
                direct_messages
            WHERE
                (sender_id = :userId OR receiver_id = :userId)
        ) AS sub
        WHERE rn = 1
        AND (created_at < :cursor OR (created_at = :cursor AND (:idAfter IS NULL OR id < :idAfter)))
        ORDER BY created_at DESC, id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<UUID> findLastMessageIdsOfConversationsWithCursor(
        @Param("userId") UUID userId,
        @Param("cursor") Instant cursor,
        @Param("idAfter") UUID idAfter,
        @Param("limit") int limit
    );

    /**
     * 2. ID 목록을 받아 DirectMessage와 연관된 User 엔티티를 JOIN FETCH로 한 번에 조회합니다.
     * N+1 문제를 완벽하게 해결합니다.
     */
    @Query("""
        SELECT DISTINCT dm
        FROM DirectMessage dm
        JOIN FETCH dm.sender
        JOIN FETCH dm.receiver
        WHERE dm.id IN :ids
        ORDER BY dm.createdAt DESC, dm.id DESC
        """)
    List<DirectMessage> findWithUsersByIds(@Param("ids") List<UUID> ids);
}
