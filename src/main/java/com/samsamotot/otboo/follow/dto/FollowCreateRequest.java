package com.samsamotot.otboo.follow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto
 * FileName     : FollowCreateRequest
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Builder
public record FollowCreateRequest(

    @NotNull
    UUID followerId,

    @NotNull
    UUID followeeId
) {
}
