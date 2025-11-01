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

    /**
     * 사용자가 참여한 대화방의 개수를 계산합니다.
     * LEAST/GREATEST + (a,b) 형태 DISTINCT로 송수신 순서와 무관하게 페어를 식별합니다.
     *
     * @param userId 현재 사용자 ID
     * @return 고유한 대화방의 총 개수
     */
    @Query(value = """
        SELECT COUNT(DISTINCT (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id)))
        FROM direct_messages
        WHERE sender_id = :userId OR receiver_id = :userId
        """, nativeQuery = true)
    long countConversations(@Param("userId") UUID userId);

    /**
     * 특정 사용자가 참여한 모든 대화방의 '가장 최신 메시지 ID' 목록을 조회합니다. (첫 페이지)
     *
     * <p><b>로직 설명:</b>
     * 1. 각 대화방(LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id)) 별로 {@code ROW_NUMBER()}를 매깁니다.
     * 2. {@code ORDER BY created_at DESC, id DESC}로 최신 메시지를 rn=1로 선정(동일 시각일 때 id로 타이브레이크).
     * 3. rn=1만 추려 최신 대화 순으로 반환합니다.
     *
     * @param userId 현재 사용자 ID
     * @param limit  최대 개수
     * @return 최신 메시지 ID의 목록
     */
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

    /**
     * 특정 사용자가 참여한 모든 대화방의 '가장 최신 메시지 ID' 목록을 커서 기반으로 조회합니다. (다음 페이지)
     *
     * <p><b>로직 설명:</b>
     * 1. 첫 페이지와 동일하게 각 대화방의 최신 메시지를 rn=1로 식별합니다.
     * 2. 커서(시간 + 보조 ID) 기준으로 다음 페이지를 조회합니다.
     *    - {@code created_at < :cursor}
     *    - 또는 {@code created_at = :cursor AND (:idAfter IS NULL OR id < :idAfter)}
     *      → idAfter가 null이어도 조건이 안전하게 평가됩니다.
     *
     * @param userId  현재 사용자 ID
     * @param cursor  이전 페이지 마지막 항목의 created_at
     * @param idAfter 동일 created_at에서의 보조 커서(이전 페이지 마지막 메시지의 id)
     * @param limit   최대 개수
     * @return 최신 메시지 ID의 목록
     */
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
     * 메시지 ID 목록으로 DirectMessage와 연관된 sender/receiver를 JOIN FETCH로 함께 조회합니다.
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
