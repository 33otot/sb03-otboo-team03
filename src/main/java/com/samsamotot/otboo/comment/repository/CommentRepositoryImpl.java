package com.samsamotot.otboo.comment.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.comment.entity.QComment;
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
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private static final String REPOSITORY = "[CommentRepositoryImpl] ";
    private final JPAQueryFactory queryFactory;
    private final QComment qComment = QComment.comment;

    /**
     * 커서 기반 페이징을 사용하여 특정 피드의 댓글 목록을 조회합니다.
     * <p>
     * <b>정렬 규약:</b>
     * 1. 생성시각(createdAt)을 기준으로 내림차순 정렬합니다.
     * 2. 생성시각이 동일할 경우, ID를 기준으로 내림차순 정렬합니다.
     * <p>
     * <b>커서 필터링 규약:</b>
     * - cursor와 idAfter가 제공되면, (createdAt < cursor) 또는 (createdAt = cursor AND id < idAfter) 조건을 만족하는 댓글만 조회합니다.
     * <p>
     * <b>페이징 규약:</b>
     * - 호출하는 쪽에서 다음 페이지 존재 여부를 확인하기 위해, 요청하려는 개수(limit)에 1을 더한 값을 limit 파라미터로 전달해야 합니다.
     *   (예: 10개를 받고 싶으면 limit = 11로 호출)
     *
     * @param feedId  조회할 피드의 ID
     * @param cursor  페이지네이션의 기준점이 되는 생성시각 (ISO 8601 형식의 문자열). 첫 페이지 조회 시에는 null.
     * @param idAfter 생성시각이 동일한 경우 2차 정렬의 기준이 되는 댓글의 ID. 첫 페이지 조회 시에는 null.
     * @param limit   조회할 최대 댓글 수 (+1)
     * @return 조회된 댓글 목록
     */
    @Override
    public List<Comment> findByFeedIdWithCursor(
        UUID feedId,
        String cursor,
        UUID idAfter,
        int limit
    ) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(qComment.feed.id.eq(feedId));

        if (StringUtils.hasText(cursor)) {
            where.and(createCursorCondition(cursor, idAfter));
            log.debug(REPOSITORY + "Applying cursor condition: {}", where);
        }

        List<Comment> result = queryFactory
            .selectFrom(qComment)
            .where(where)
            .orderBy(qComment.createdAt.desc(), qComment.id.desc())
            .limit(limit)
            .fetch();

        log.debug(REPOSITORY + "findByFeedIdWithCursor result size: {}", result.size());
        return result;
    }

    private BooleanExpression createCursorCondition(String cursor, UUID idAfter) {
        Instant createdAt = Instant.parse(cursor);

        if (idAfter == null) {
            return qComment.createdAt.lt(createdAt);
        }

        return qComment.createdAt.lt(createdAt)
            .or(qComment.createdAt.eq(createdAt).and(qComment.id.lt(idAfter)));
    }
}
