package com.samsamotot.otboo.directmessage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.samsamotot.otboo.user.dto.AuthorDto;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : DirectMessageDto
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Builder
public record DirectMessageDto(
    UUID id,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant createdAt,
    AuthorDto sender,
    AuthorDto receiver,
    String content
) {
}
