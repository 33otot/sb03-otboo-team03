package com.samsamotot.otboo.follow.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestParam;

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

    String idAfter,

    @NotNull
    Integer limit,

    String nameLike
) {
}
