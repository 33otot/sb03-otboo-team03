package com.samsamotot.otboo.follow.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.follow.dto.FollowingRequest;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.follow.entity.QFollow;
import com.samsamotot.otboo.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepositoryImpl
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowRepositoryCustom {

    private static final String LISTENER_NAME = "[FollowRepositoryImpl] ";

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 사용자가 팔로우하는 대상의 총 개수를 조회한다.
     *
     * @param userId   팔로워 사용자 ID
     * @param nameLike 팔로우 대상 사용자명 검색 조건 (nullable)
     * @return 팔로잉 총 개수
     */
    @Override
    public long countTotalElements(UUID userId, String nameLike) {
        log.info(LISTENER_NAME + "총 개수 조회 시작: userId={}, nameLike={}", userId, nameLike);

        QFollow follow = QFollow.follow;
        QUser followee = new QUser("followee");

        BooleanBuilder where = new BooleanBuilder().and(follow.follower.id.eq(userId));

        boolean hasName = nameLike != null && !nameLike.isBlank();
        if (hasName) {
            where.and(followee.username.containsIgnoreCase(nameLike));
        }

        Long result = (hasName
            ? queryFactory.select(follow.count()).from(follow).join(follow.followee, followee).where(where)
            : queryFactory.select(follow.count()).from(follow).where(where)
        ).fetchOne();

        long count = result != null ? result : 0;
        log.info(LISTENER_NAME + "총 개수 조회 완료: userId={}, count={}", userId, count);

        return count;
    }

    /**
     * 특정 사용자가 팔로우하는 목록을 페이징 조회한다.
     * <p>
     * 커서 기반 페이지네이션(cursor, idAfter)과 이름 검색 조건을 지원한다.
     *
     * @param request 팔로잉 조회 요청 정보
     * @return 팔로우 엔티티 목록 (limit+1 개)
     */
    @Override
    public List<Follow> findFollowings(FollowingRequest request) {
        log.info(LISTENER_NAME + "팔로잉 목록 조회 시작: followerId={}, limit={}, cursor={}, idAfter={}, nameLike={}",
            request.followerId(), request.limit(), request.cursor(), request.idAfter(), request.nameLike());

        QFollow follow = QFollow.follow;

        QUser followee = new QUser("followee");
        QUser follower = new QUser("follower");

        BooleanBuilder baseWhere = new BooleanBuilder()
            .and(follow.follower.id.eq(request.followerId()));

        // nameLike 조건
        if (request.nameLike() != null && !request.nameLike().isBlank()) {
            baseWhere.and(follow.followee.username.containsIgnoreCase(request.nameLike()));
        }

        // 타이 브레이커
        BooleanBuilder where = new BooleanBuilder().and(baseWhere);
        Instant cursorCreatedAt = parseCursorToInstant(request.cursor());
        UUID cursorId = request.idAfter();
        if (cursorCreatedAt != null && cursorId != null) {
            where.and(
                follow.createdAt.lt(cursorCreatedAt)
                    .or(follow.createdAt.eq(cursorCreatedAt).and(follow.id.lt(cursorId)))
            );
        } else if (cursorCreatedAt != null) {
            where.and(follow.createdAt.lt(cursorCreatedAt));
        } else if (cursorId != null) {
            where.and(follow.id.lt(cursorId));
        }

        int limit = Math.max(1, request.limit()) + 1;

        List<Follow> rows = queryFactory
            .selectFrom(follow)
            .join(follow.followee, followee).fetchJoin()
            .join(follow.follower, follower).fetchJoin()
            .where(where)
            .orderBy(follow.createdAt.desc(), follow.id.desc())
            .limit(limit)
            .fetch();

        log.info(LISTENER_NAME + "팔로잉 목록 조회 완료: followerId={}, 결과 수={}", request.followerId(), rows.size());

        return rows;
    }

    /**
     * 문자열 커서를 Instant로 파싱한다.
     * ISO-8601, OffsetDateTime, LocalDateTime 형식을 지원한다.
     *
     * @param cursor 커서 문자열
     * @return 변환된 Instant, 실패 시 null
     */
    private static Instant parseCursorToInstant(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return Instant.parse(cursor);
        } catch (Exception ignore) {
        }
        try {
            return OffsetDateTime.parse(cursor).toInstant();
        } catch (Exception ignore) {
        }
        try {
            return LocalDateTime.parse(cursor).toInstant(ZoneOffset.UTC);
        } catch (Exception ignore) {
        }
        return null;
    }
}
