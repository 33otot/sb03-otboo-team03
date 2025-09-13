package com.samsamotot.otboo.follow.dto;

import com.samsamotot.otboo.user.entity.User;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto
 * FileName     : FollowDto
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
public record FollowDto(
    UUID id,
    User followee,
    User follower
) {
}
