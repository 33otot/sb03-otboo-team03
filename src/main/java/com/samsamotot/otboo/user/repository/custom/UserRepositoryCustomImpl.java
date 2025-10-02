package com.samsamotot.otboo.user.repository.custom;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.user.dto.UserListRequest;
import com.samsamotot.otboo.user.entity.QUser;
import com.samsamotot.otboo.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 Repository 커스텀 구현체
 * 
 * <p>커서 기반 페이지네이션을 사용한 사용자 목록 조회 기능을 구현합니다.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QUser qUser = QUser.user;

    /**
     * 커서 기반 페이지네이션을 사용하여 사용자 목록을 조회합니다.
     * 
     * @param request 사용자 목록 조회 요청 파라미터
     * @return 사용자 목록과 페이지네이션 정보를 포함하는 Slice
     */
    @Override
    public Slice<User> findUsersWithCursor(UserListRequest request) {
        log.debug("[UserRepositoryCustomImpl] 사용자 목록 조회 시작: {}", request);

        // 필터링 조건 생성
        BooleanBuilder where = createFilterCondition(request);
        log.debug("[UserRepositoryCustomImpl] 필터 조건: {}", where);

        // 커서 조건 추가
        if (StringUtils.hasText(request.cursor()) && request.idAfter() != null) {
            where.and(createCursorCondition(request));
            log.debug("[UserRepositoryCustomImpl] 커서 조건 추가됨");
        }

        // 정렬 조건 생성
        OrderSpecifier<?> orderSpecifier = createOrderSpecifier(request);
        OrderSpecifier<?> idOrderSpecifier = getSortDirection(request) == SortDirection.ASCENDING 
            ? qUser.id.asc() 
            : qUser.id.desc();

        int pageSize = request.limit();
        
        // limit + 1로 조회하여 hasNext 판단
        List<User> content = queryFactory
            .selectFrom(qUser)
            .where(where)
            .orderBy(orderSpecifier, idOrderSpecifier)
            .limit(pageSize + 1)
            .fetch();

        // hasNext 판단
        boolean hasNext = content.size() > pageSize;
        if (hasNext) {
            content = content.subList(0, pageSize);
        }

        Pageable pageable = PageRequest.of(0, pageSize);
        log.debug("[UserRepositoryCustomImpl] 사용자 목록 조회 완료: 조회된 수={}, hasNext={}", content.size(), hasNext);

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * 필터링 조건에 맞는 전체 사용자 수를 조회합니다.
     * 
     * @param request 사용자 목록 조회 요청 파라미터
     * @return 전체 사용자 수
     */
    @Override
    public long countUsersWithFilters(UserListRequest request) {
        log.debug("[UserRepositoryCustomImpl] 사용자 수 조회 시작: {}", request);

        BooleanBuilder where = createFilterCondition(request);
        
        Long count = queryFactory
            .select(qUser.count())
            .from(qUser)
            .where(where)
            .fetchOne();

        log.debug("[UserRepositoryCustomImpl] 사용자 수 조회 완료: {}", count);
        return count != null ? count : 0L;
    }

    /**
     * 필터링 조건을 생성합니다.
     * 
     * @param request 사용자 목록 조회 요청 파라미터
     * @return 필터링 조건
     */
    private BooleanBuilder createFilterCondition(UserListRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        // 이메일 검색
        if (StringUtils.hasText(request.emailLike())) {
            builder.and(qUser.email.containsIgnoreCase(request.emailLike()));
        }

        // 권한 필터
        if (request.roleEqual() != null) {
            builder.and(qUser.role.eq(request.roleEqual()));
        }

        // 잠금 상태 필터
        if (request.locked() != null) {
            builder.and(qUser.isLocked.eq(request.locked()));
        }

        return builder;
    }

    /**
     * 커서 조건을 생성합니다.
     * 
     * @param request 사용자 목록 조회 요청 파라미터
     * @return 커서 조건
     */
    private BooleanExpression createCursorCondition(UserListRequest request) {
        String cursor = request.cursor();
        UUID idAfter = request.idAfter();
        String sortBy = request.sortBy();
        SortDirection sortDirection = getSortDirection(request);

        if ("email".equals(sortBy)) {
            if (sortDirection == SortDirection.ASCENDING) {
                return qUser.email.gt(cursor)
                    .or(qUser.email.eq(cursor).and(qUser.id.gt(idAfter)));
            } else {
                return qUser.email.lt(cursor)
                    .or(qUser.email.eq(cursor).and(qUser.id.lt(idAfter)));
            }
        } else { // createdAt
            try {
                Instant cursorTime = Instant.parse(cursor);
                if (sortDirection == SortDirection.ASCENDING) {
                    return qUser.createdAt.gt(cursorTime)
                        .or(qUser.createdAt.eq(cursorTime).and(qUser.id.gt(idAfter)));
                } else {
                    return qUser.createdAt.lt(cursorTime)
                        .or(qUser.createdAt.eq(cursorTime).and(qUser.id.lt(idAfter)));
                }
            } catch (Exception e) {
                log.warn("[UserRepositoryCustomImpl] 잘못된 커서 형식: {}", cursor);
                return null;
            }
        }
    }

    /**
     * 정렬 조건을 생성합니다.
     * 
     * @param request 사용자 목록 조회 요청 파라미터
     * @return 정렬 조건
     */
    private OrderSpecifier<?> createOrderSpecifier(UserListRequest request) {
        String sortBy = request.sortBy();
        SortDirection sortDirection = getSortDirection(request);

        if ("email".equals(sortBy)) {
            return sortDirection == SortDirection.ASCENDING 
                ? qUser.email.asc() 
                : qUser.email.desc();
        } else { // createdAt
            return sortDirection == SortDirection.ASCENDING 
                ? qUser.createdAt.asc() 
                : qUser.createdAt.desc();
        }
    }

    /**
     * 정렬 방향을 반환합니다.
     * 
     * @param request 사용자 목록 조회 요청 파라미터
     * @return 정렬 방향
     */
    private SortDirection getSortDirection(UserListRequest request) {
        return "ASCENDING".equalsIgnoreCase(request.sortDirection()) 
            ? SortDirection.ASCENDING 
            : SortDirection.DESCENDING;
    }
}
