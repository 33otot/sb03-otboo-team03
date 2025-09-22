package com.samsamotot.otboo.directmessage.dto;

import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.follow.dto.FollowDto;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : DirectMessageListResponse
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Builder
public record DirectMessageListResponse(
    List<DirectMessageDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {
}