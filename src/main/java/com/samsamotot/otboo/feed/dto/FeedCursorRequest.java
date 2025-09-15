package com.samsamotot.otboo.feed.dto;

import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Builder;

/**
 * 피드 목록 조회 파라미터 (query)
 * - 기본 정렬 기준: createdAt
 * - 기본 정렬 방향 : DESCENDING
 * - 기본 limit: 10
 */
@Builder
public record FeedCursorRequest(
    String cursor,
    UUID idAfter,
    @Positive
    Integer limit,
    @NotNull
    String sortBy,
    @NotNull
    SortDirection sortDirection,
    String keywordLike,
    SkyStatus skyStatusEqual,
    Precipitation precipitationTypeEqual,
    UUID authorIdEqual
) {
}