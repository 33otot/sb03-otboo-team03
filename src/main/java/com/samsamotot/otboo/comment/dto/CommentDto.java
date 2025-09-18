package com.samsamotot.otboo.comment.dto;

import com.samsamotot.otboo.user.dto.AuthorDto;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CommentDto(
    UUID id,
    Instant createdAt,
    UUID feedId,
    AuthorDto author,
    String content
) {

}
