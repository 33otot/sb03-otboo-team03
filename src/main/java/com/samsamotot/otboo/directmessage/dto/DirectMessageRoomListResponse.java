package com.samsamotot.otboo.directmessage.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record DirectMessageRoomListResponse(
    List<DirectMessageRoomDto> rooms
) {
}
