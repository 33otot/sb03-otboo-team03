package com.samsamotot.otboo.follow.dto;

import jakarta.validation.constraints.AssertTrue;
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

    @AssertTrue(message = "followerId는 followeeId와 달라야 합니다.")
    public boolean isNotSelfFollow() {
        return followerId != null && followeeId != null && !followerId.equals(followeeId);
    }

}
