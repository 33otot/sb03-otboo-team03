package com.samsamotot.otboo.directmessage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.samsamotot.otboo.user.dto.AuthorDto;
import lombok.Builder;

import java.time.Instant;

@Builder
public record DirectMessageRoomDto(
    AuthorDto partner,
    String lastMessage,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant lastMessageSentAt
) {
}
