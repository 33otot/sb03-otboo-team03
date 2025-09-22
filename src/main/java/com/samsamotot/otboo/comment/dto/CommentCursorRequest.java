package com.samsamotot.otboo.comment.dto;

import jakarta.validation.constraints.AssertTrue;
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
    /**
     * idAfter 파라미터는 cursor 파라미터와 함께 제공되어야 합니다.
     */
    @AssertTrue(message = "idAfter 파라미터는 cursor 파라미터와 함께 제공되어야 합니다.")
    private boolean isCursorPairValid() {
        return idAfter == null || cursor != null;
    }
}
