package com.samsamotot.otboo.directmessage.dto;

import com.samsamotot.otboo.user.dto.AuthorDto;
import lombok.Builder;

import java.time.Instant;

@Builder
public record DirectMessageRoomDto(
    AuthorDto partner,
    String lastMessage,
    Instant lastMessageSentAt
) {
}
