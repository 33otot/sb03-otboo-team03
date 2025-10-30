package com.samsamotot.otboo.feed.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeletedFeedCursorRequest(
    String cursor,
    UUID idAfter,
    @Positive @Max(50)
    Integer limit
) {
}
