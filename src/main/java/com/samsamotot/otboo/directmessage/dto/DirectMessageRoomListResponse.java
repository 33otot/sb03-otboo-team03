package com.samsamotot.otboo.directmessage.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record DirectMessageRoomListResponse(
    List<DirectMessageRoomDto> rooms
) {
}
