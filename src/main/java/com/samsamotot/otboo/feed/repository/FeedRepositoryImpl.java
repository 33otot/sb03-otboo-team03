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
import java.time.format.DateTimeParseException;
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

    /**
     * 커서 기반 페이지네이션을 사용하여 피드 목록을 조회합니다.
     * 다양한 필터링 조건(키워드, 날씨 상태, 강수 형태, 작성자 ID)과 정렬 기준을 적용할 수 있습니다.
     * (소프트 삭제된 피드는 기본으로 제외됨)
     *
     * @param cursor                    페이지네이션의 기준 커서 값 (createdAt 또는 likeCount)
     * @param idAfter                   마지막으로 조회된 피드 ID
     * @param limit                     조회할 피드의 최대 개수
     * @param sortBy                    정렬 기준 필드 (예: "createdAt", "likeCount")
     * @param sortDirection             정렬 방향 (ASCENDING 또는 DESCENDING)
     * @param keywordLike               피드 내용에 포함될 키워드 (부분 일치)
     * @param skyStatusEqual            날씨 상태 필터링 (예: SUNNY, CLOUDY)
     * @param precipitationTypeEqual    강수 형태 필터링 (예: NONE, RAIN, SNOW)
     * @param authorIdEqual             특정 작성자의 피드만 조회할 경우 작성자 ID
     * @return 조회된 피드 엔티티 목록
     */
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
        log.debug("[FeedRepositoryImpl] Filter condition (before cursor): {}", where);

        // 커서 조건 적용
        if (cursor != null && !cursor.isBlank()) {
            if (idAfter == null) {
                throw new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT);
            }
            where.and(createCursorCondition(sortBy, sortDirection, cursor, idAfter));
            log.debug("[FeedRepositoryImpl] Filter condition (after cursor): {}", where);
        }
        // 정렬 조건 생성
        OrderSpecifier<?> orderSpecifier = createOrderSpecifier(sortBy, sortDirection);
        // ID 정렬 조건
        OrderSpecifier<?> idOrderSpecifier = sortDirection == SortDirection.ASCENDING ? qFeed.id.asc() : qFeed.id.desc();

        List<Feed> result = queryFactory
            .selectFrom(qFeed)
            .where(where)
            .orderBy(orderSpecifier, idOrderSpecifier)
            .limit(limit)
            .fetch();

        log.debug("[FeedRepositoryImpl] findByCursor result size: {}", result.size());
        return result;
    }

    /**
     * 주어진 필터링 조건에 해당하는 피드의 총 개수를 조회합니다.
     *
     * @param keywordLike               피드 내용에 포함될 키워드 (부분 일치)
     * @param skyStatusEqual            날씨 상태 필터링 (예: SUNNY, CLOUDY)
     * @param precipitationTypeEqual    강수 형태 필터링 (예: NONE, RAIN, SNOW)
     * @param authorIdEqual             특정 작성자의 피드만 조회할 경우 작성자 ID
     * @return 필터링 조건에 맞는 피드의 총 개수
     */
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

        Instant createdAt;
        try {
            createdAt = Instant.parse(cursor);
        } catch(DateTimeParseException e) {
            throw new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT);
        }

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
