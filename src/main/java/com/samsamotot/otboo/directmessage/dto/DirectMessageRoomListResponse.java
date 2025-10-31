package com.samsamotot.otboo.directmessage.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageRoomListResponse(
    List<DirectMessageRoomDto> rooms,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {
}