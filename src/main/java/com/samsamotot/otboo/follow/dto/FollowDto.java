package com.samsamotot.otboo.follow.dto;

import com.samsamotot.otboo.follow.dto.user.UserSummaryDto;
import lombok.Builder;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto
 * FileName     : FollowDto
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Builder
public record FollowDto(
    UUID id,
    UserSummaryDto followee,
    UserSummaryDto follower
) {
}
