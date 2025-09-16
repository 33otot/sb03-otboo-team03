package com.samsamotot.otboo.follow.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto
 * FileName     : FollowListResponse
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
@Builder
public record FollowListResponse(
    List<FollowDto> data,
    String nextCursor, // nullable
    UUID nextIdAfter, // nullable
    boolean hasNext,
    long totalCount,
    String sortBy, // createdAt of Follow
    String sortDirection  // DESCENDING only
    ) {
}