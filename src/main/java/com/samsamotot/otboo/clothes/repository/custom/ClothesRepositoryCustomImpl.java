package com.samsamotot.otboo.clothes.repository.custom;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.clothes.dto.request.ClothesSearchRequest;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.QClothes;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Slf4j
@RequiredArgsConstructor
@Repository
public class ClothesRepositoryCustomImpl implements ClothesRepositoryCustom {

    @PersistenceContext
    private final JPAQueryFactory queryFactory;
    private final QClothes clothes = QClothes.clothes;

    // 의상 목록 조회하는 QueryDsl 메서드
    @Override
    public Slice<Clothes> findClothesWithCursor(ClothesSearchRequest request, Pageable pageable) {

        BooleanBuilder builder = buildCondition(request);

        // 커서 조건 추가
        cursorCondition(builder, request);

        int pageSize = pageable.getPageSize();

        // 만약 조건 없다면 전체 조회
        List<Clothes> content = queryFactory
            .selectFrom(clothes)
            .where(builder.hasValue() ? builder : null)
            .orderBy(clothes.createdAt.desc(), clothes.id.desc())
            .limit(pageSize + 1)
            .fetch();

        // hasNext 판단
        boolean hasNext = false;
        if (content.size() > pageSize) {
            // 다음 요소 있는 거 확인했으니까 초과된 content는 없애버리기
            content.remove(pageSize);
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 커서 조건 추가
    void cursorCondition(BooleanBuilder builder, ClothesSearchRequest request) {
        // 마지막 요소의 createdAt
        String cursorStr = request.cursor();

        UUID idAfter = request.idAfter();

        // 첫 페이지라면 조건 추가 안 함
        if (cursorStr == null || cursorStr.isBlank()) {
            return;
        }

        try {
            Instant cursor = Instant.parse(cursorStr);

            if (idAfter != null) {
                builder.and(
                    clothes.createdAt.lt(cursor)
                        .or(clothes.createdAt.eq(cursor)
                            .and(clothes.id.lt(idAfter)))
                );
            } else {
                builder.and(clothes.createdAt.lt(cursor));
            }
        } catch (Exception e){
            log.warn("커서 조건을 위한 값 파싱 실패: {} ", request.cursor());
        }
    }

    // 조회 조건 build에 넣기
    BooleanBuilder buildCondition(ClothesSearchRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(clothes.owner.id.eq(request.ownerId()));

        // 의상 타입 일치 (완전 일치)
        if (request.typeEqual() != null){
            builder.and(clothes.type.eq(request.typeEqual()));
        }

        return builder;
    }

    // totalElement 구하기
    @Override
    public long totalElementCount(ClothesSearchRequest request) {

        BooleanBuilder builder = buildCondition(request);

        Long count = queryFactory
            .select(clothes.count())
            .from(clothes)
            .where(builder.hasValue() ? builder : null)
            .fetchOne();

        return count != null ? count : 0L;
    }
}
