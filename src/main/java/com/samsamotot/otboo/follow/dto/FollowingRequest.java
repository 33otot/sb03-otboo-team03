package com.samsamotot.otboo.follow.dto;

import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto
 * FileName     : FollowingRequest
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
public record FollowingRequest(
    @NotNull
    UUID followerId,

    String cursor,

    UUID idAfter,

    @NotNull
    @Min(1)
    @Max(100)
    Integer limit,

    @Size(max = 100)
    String nameLike
) {
    @AssertTrue(message = "cursor와 idAfter는 둘 다 null이거나 둘 다 값이 있어야 합니다.")
    public boolean isCursorPairConsistent() {
        return (cursor == null && idAfter == null) || (cursor != null && idAfter != null);
    }
}
