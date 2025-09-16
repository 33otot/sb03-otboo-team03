package com.samsamotot.otboo.follow.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.follow.dto.FollowingRequest;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.follow.entity.QFollow;
import com.samsamotot.otboo.user.entity.QUser;
import lombok.RequiredArgsConstructor;
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
@Repository
@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countTotalElements(UUID userId, String nameLike) {
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

        return result != null ? result : 0;
    }

    @Override
    public List<Follow> findFollowings(FollowingRequest request) {

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

        return rows;
    }

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
