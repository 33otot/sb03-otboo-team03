package com.samsamotot.otboo.notification.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto
 * FileName     : NotificationRequest
 * Author       : dounguk
 * Date         : 2025. 9. 30.
 */
public record NotificationRequest(
    Instant cursor,

    UUID idAfter,

    @NotNull
    @Min(1)
    @Max(100)
    Integer limit
) {
    @AssertTrue(message = "cursor와 idAfter는 둘 다 null이거나 둘 다 값이 있어야 합니다.")
    public boolean isCursorPairConsistent() {
        return (cursor == null && idAfter == null) || (cursor != null && idAfter != null);
    }
}