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


    /*
        대화방 목록 조회
        - 사용자가 참여한 모든 대화(sender or receiver)를 조회
        - 각 대화의 가장 최근 메시지 1개만 선택
     */
    @Query(value = """
        WITH ranked_messages AS (
            SELECT
                m.*,
                ROW_NUMBER() OVER(PARTITION BY
                    CASE WHEN sender_id > receiver_id THEN CAST(sender_id AS VARCHAR) ELSE CAST(receiver_id AS VARCHAR) END,
                    CASE WHEN sender_id > receiver_id THEN CAST(receiver_id AS VARCHAR) ELSE CAST(sender_id AS VARCHAR) END
                ORDER BY created_at DESC) as rn
            FROM direct_messages m
            WHERE m.sender_id = :userId OR m.receiver_id = :userId
        )
        SELECT * FROM ranked_messages WHERE rn = 1 ORDER BY created_at DESC
    """, nativeQuery = true)
    List<DirectMessage> findConversationsByUserId(@Param("userId") UUID userId);
}
