package com.samsamotot.otboo.common.dto;

import java.util.List;
import java.util.UUID;

import com.samsamotot.otboo.common.type.SortDirection;

import lombok.Builder;

@Builder
public record CursorResponse<T>(
        List<T> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        SortDirection sortDirection) {
}