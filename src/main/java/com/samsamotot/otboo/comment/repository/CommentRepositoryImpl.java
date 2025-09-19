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

@Slf4j
@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final String REPOSITORY = "[CommentRepositoryImpl] ";
    private final JPAQueryFactory queryFactory;
    private final QComment qComment = QComment.comment;

    /**
     * 커서 기반 페이지네이션을 사용하여 특정 피드의 댓글 목록을 조회합니다.
     * 정렬 순서는 생성시각(createdAt) 내림차순, 그리고 ID(id) 내림차순입니다.
     * 커서 값이 존재할 경우, 해당 커서 위치보다 오래된 댓글들을 조회합니다.
     *
     * @param feedId    조회할 피드의 ID
     * @param cursor    이전 페이지의 마지막 댓글 생성 시각
     * @param idAfter   이전 페이지의 마지막 댓글 ID
     * @param limit     조회할 최대 댓글 수
     * @return 조회된 댓글 엔티티 목록
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

        if (cursor != null && !cursor.isEmpty()) {
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
