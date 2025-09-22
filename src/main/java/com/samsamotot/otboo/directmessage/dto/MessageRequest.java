package com.samsamotot.otboo.directmessage.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : MessageRequest
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
public record MessageRequest (
    @NotNull
    UUID followerId,

    String cursor,

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





