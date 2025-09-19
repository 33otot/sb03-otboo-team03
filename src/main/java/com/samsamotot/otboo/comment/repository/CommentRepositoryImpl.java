package com.samsamotot.otboo.comment.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.comment.entity.QComment;
import com.samsamotot.otboo.common.type.SortDirection;
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
