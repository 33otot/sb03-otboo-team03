package com.samsamotot.otboo.feed.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.QFeed;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private final QFeed qFeed = QFeed.feed;

    @Override
    public List<Feed> findByCursor(
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        SortDirection sortDirection,
        String keywordLike,
        SkyStatus skyStatusEqual,
        Precipitation precipitationTypeEqual,
        UUID authorIdEqual
    ) {
        // 필터링 조건 적용
        BooleanBuilder where = createFilterCondition(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);
        // 커서 조건 적용
        if (cursor != null && !cursor.isBlank()) {
            where.and(createCursorCondition(sortBy, sortDirection, cursor, idAfter));
        }
        // 정렬 조건 생성
        OrderSpecifier<?> orderSpecifier = createOrderSpecifier(sortBy, sortDirection);
        // ID 정렬 조건
        OrderSpecifier<?> idOrderSpecifier = sortDirection == SortDirection.ASCENDING ? qFeed.id.asc() : qFeed.id.desc();

        return queryFactory
            .selectFrom(qFeed)
            .where(where)
            .orderBy(orderSpecifier, idOrderSpecifier)
            .limit(limit)
            .fetch();
    }

    @Override
    public long countByFilter(String keywordLike, SkyStatus skyStatusEqual,
        Precipitation precipitationTypeEqual, UUID authorIdEqual
    ) {
        BooleanBuilder where = createFilterCondition(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);

        Long count = queryFactory
            .select(qFeed.count())
            .from(qFeed)
            .where(where)
            .fetchOne();

        return count != null ? count : 0L;
    }

    private BooleanBuilder createFilterCondition(
        String keywordLike, SkyStatus skyStatusEqual, Precipitation precipitationTypeEqual, UUID authorIdEqual
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        // 논리 삭제
        builder.and(qFeed.isDeleted.isFalse());

        if (StringUtils.hasText(keywordLike)) {
            builder.and(qFeed.content.containsIgnoreCase(keywordLike));
        }

        if (skyStatusEqual != null) {
            builder.and(qFeed.weather.skyStatus.eq(skyStatusEqual));
        }

        if (precipitationTypeEqual != null) {
            builder.and(qFeed.weather.precipitationType.eq(precipitationTypeEqual));
        }

        if (authorIdEqual != null) {
            builder.and(qFeed.author.id.eq(authorIdEqual));
        }

        return builder;
    }

    private BooleanExpression createCursorCondition(String sortBy, SortDirection sortDirection, String cursor, UUID idAfter) {

        return switch (sortBy) {
            case "createdAt" -> buildCreatedAtCondition(sortDirection, cursor, idAfter);
            case "likeCount" -> buildLikeCountCondition(sortDirection, cursor, idAfter);
            default -> throw new OtbooException(ErrorCode.INVALID_SORT_FIELD);
        };
    }

    private BooleanExpression buildCreatedAtCondition(SortDirection sortDirection, String cursor, UUID idAfter) {
        Instant createdAt = Instant.parse(cursor);

        if (sortDirection == SortDirection.ASCENDING) {
            return qFeed.createdAt.gt(createdAt)
                .or(qFeed.createdAt.eq(createdAt).and(qFeed.id.gt(idAfter)));
        } else {
            return qFeed.createdAt.lt(createdAt)
                .or(qFeed.createdAt.eq(createdAt).and(qFeed.id.lt(idAfter)));
        }
    }

    private BooleanExpression buildLikeCountCondition(SortDirection sortDirection, String cursor, UUID idAfter) {
        long likeCount = Long.parseLong(cursor);

        if (sortDirection == SortDirection.ASCENDING) {
            return qFeed.likeCount.gt(likeCount)
                .or(qFeed.likeCount.eq(likeCount).and(qFeed.id.gt(idAfter)));
        } else {
            return qFeed.likeCount.lt(likeCount)
                .or(qFeed.likeCount.eq(likeCount).and(qFeed.id.lt(idAfter)));
        }
    }

    private OrderSpecifier<?> createOrderSpecifier(String sortBy, SortDirection sortDirection) {
        Order order = sortDirection == SortDirection.ASCENDING ? Order.ASC : Order.DESC;

        return switch (sortBy) {
            case "createdAt" -> new OrderSpecifier<>(order, qFeed.createdAt);
            case "likeCount" -> new OrderSpecifier<>(order, qFeed.likeCount);
            default -> throw new OtbooException(ErrorCode.INVALID_SORT_FIELD);
        };
    }
}
