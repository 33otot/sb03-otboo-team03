package com.samsamotot.otboo.follow.dto;

import com.samsamotot.otboo.common.type.SortDirection;
import io.micrometer.common.lang.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto
 * FileName     : FollowListResponse
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
public record FollowListResponse(
    List<FollowDto> data,
    String nextCursor, // nullable
    UUID nextIdAfter, // nullable
    boolean hasNext,
    String sortBy, // createdAt of Follow
    SortDirection sortDirection,  //DESCENDING only
    long totalCount
    ) {
}