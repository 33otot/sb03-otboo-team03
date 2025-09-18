package com.samsamotot.otboo.comment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Builder;

/**
 * 피드 댓글 목록 조회 파라미터 (query)
 */
@Builder
public record CommentCursorRequest(
    String cursor,
    UUID idAfter,
    @NotNull @Positive @Max(50)
    Integer limit
) {

}
