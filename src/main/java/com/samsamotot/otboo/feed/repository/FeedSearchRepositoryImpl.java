package com.samsamotot.otboo.feed.repository;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.document.FeedDocument;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.mapper.FeedDocumentMapper;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedSearchRepositoryImpl implements FeedSearchRepositoryCustom {

    private final ElasticsearchOperations operations;
    private final FeedDocumentMapper feedDocumentMapper;

    private static final String FIELD_CREATED_AT   = "createdAt";
    private static final String FIELD_LIKE_COUNT   = "likeCount";
    private static final String FIELD_CONTENT      = "content";
    private static final String FIELD_AUTHOR_ID    = "author.userId";
    private static final String FIELD_SKY_STATUS   = "weather.skyStatus";
    private static final String FIELD_PRECIP_TYPE  = "weather.precipitation.type";
    private final String REPOSITORY = "[FeedSearchRepository] ";

    @Override
    public CursorResponse<FeedDto> findByCursor(
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
        BoolQuery.Builder bool = createFilterCondition(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);
        log.debug(REPOSITORY + "필터 조건 생성: {}", bool);

        // 정렬 필드/방향 설정
        String sortField = switch (sortBy) {
            case "createdAt" -> FIELD_CREATED_AT;
            case "likeCount" -> FIELD_LIKE_COUNT;
            default -> throw new OtbooException(ErrorCode.INVALID_SORT_FIELD);
        };
        boolean asc = (sortDirection == SortDirection.ASCENDING);

        List<SortOptions> sorts = List.of(
            SortOptions.of(s -> s.field(f -> f.field(sortField).order(asc ? SortOrder.Asc : SortOrder.Desc))),
            SortOptions.of(s -> s.field(f -> f.field("id").order(asc ? SortOrder.Asc : SortOrder.Desc)))
        );
        log.info(REPOSITORY + "정렬 조건 생성: {}", sorts);

        // 커서(search_after) 준비
        List<Object> searchAfter = null;
        if (StringUtils.hasText(cursor)) {
            if (idAfter == null) throw new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT);

            Object primaryKeyValue = parseCursorValue(cursor, sortField);
            searchAfter = List.of(primaryKeyValue, idAfter.toString());
            log.info(REPOSITORY + "search_after 값: {}", searchAfter);
        }

        int effectiveLimit = Math.max(1, limit);
        int pageSize = effectiveLimit + 1;

        // NativeQuery 조립: 필터 + 정렬 + 페이지(size) + search_after
        NativeQueryBuilder nq = NativeQuery.builder()
            .withQuery(q -> q.bool(bool.build()))
            .withSort(sorts)
            .withPageable(PageRequest.of(0, pageSize));

        if (searchAfter != null) {
            nq.withSearchAfter(searchAfter);
        }

        SearchHits<FeedDocument> hits = operations.search(nq.build(), FeedDocument.class);
        log.info(REPOSITORY + "Elasticsearch 검색 결과: {} hits", hits.getTotalHits());
        if (hits.isEmpty()) {
            return new CursorResponse<>(
                List.of(), null, null, false,
                0L,
                sortBy, sortDirection
            );
        }

        List<FeedDto> feedDtos = hits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(feedDocumentMapper::toDto)
            .collect(Collectors.toList());

        boolean hasNext = feedDtos.size() > effectiveLimit;
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext) {
            FeedDto lastFeedDto =  feedDtos.get(effectiveLimit - 1);
            nextCursor = resolveNextCursor(lastFeedDto, sortBy);
            nextIdAfter = lastFeedDto.id();
            feedDtos = feedDtos.subList(0, effectiveLimit);
        }

        long totalCount = countByFilter(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);

        return new CursorResponse<>(
            feedDtos,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }

    private long countByFilter(
        String keywordLike,
        SkyStatus skyStatusEqual,
        Precipitation precipitationTypeEqual,
        UUID authorIdEqual
    ) {
        BoolQuery.Builder bool = createFilterCondition(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);
        NativeQuery nq = NativeQuery.builder()
            .withQuery(q -> q.bool(bool.build()))
            .build();
        return operations.count(nq, FeedDocument.class);
    }

    private BoolQuery.Builder createFilterCondition(
        String keywordLike, SkyStatus skyStatusEqual, Precipitation precipitationTypeEqual, UUID authorIdEqual
    ) {
        BoolQuery.Builder bool = QueryBuilders.bool();

        // 논리 삭제 피드 제외
        bool.must(m -> m.term(t -> t.field("isDeleted").value(false)));

        if (StringUtils.hasText(keywordLike)) {
            bool.must(m -> m.match(mm -> mm
                .field(FIELD_CONTENT)
                .query(keywordLike)
                .fuzziness("AUTO")
                .lenient(true)
            ));
        }
        if (skyStatusEqual != null) {
            bool.filter(f -> f.term(t -> t.field(FIELD_SKY_STATUS).value(skyStatusEqual.name())));
        }
        if (precipitationTypeEqual != null) {
            bool.filter(f -> f.term(t -> t.field(FIELD_PRECIP_TYPE).value(precipitationTypeEqual.name())));
        }
        if (authorIdEqual != null) {
            bool.filter(f -> f.term(t -> t.field(FIELD_AUTHOR_ID).value(authorIdEqual.toString())));
        }

        return bool;
    }

    private Object parseCursorValue(String cursor, String sortField) {
        if (FIELD_CREATED_AT.equals(sortField)) {
            try {
                // ISO 8601 형식의 날짜 문자열을 epoch millisecond로 변환
                return Instant.parse(cursor).toEpochMilli();
            } catch (DateTimeParseException e) {
                throw new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT);
            }
        }
        // 그 외 필드 (likeCount 등)
        return parseAsLong(cursor);
    }

    private String resolveNextCursor(FeedDto feedDto, String sortBy) {
        return switch(sortBy) {
            case FIELD_CREATED_AT -> feedDto.createdAt().toString();
            case FIELD_LIKE_COUNT -> String.valueOf(feedDto.likeCount());
            default -> throw new OtbooException(ErrorCode.INVALID_SORT_FIELD);
        };
    }

    private Long parseAsLong(String cursor) {
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }
}