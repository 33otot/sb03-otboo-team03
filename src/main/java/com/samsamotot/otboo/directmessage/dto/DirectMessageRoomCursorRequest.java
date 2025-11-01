package com.samsamotot.otboo.directmessage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageRoomCursorRequest(
    Instant cursor,
    UUID idAfter,
    @NotNull @Positive @Max(50)
    Integer limit
) {

}
