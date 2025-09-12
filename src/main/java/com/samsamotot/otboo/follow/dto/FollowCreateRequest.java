package com.samsamotot.otboo.follow.dto;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto
 * FileName     : FollowCreateRequest
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
public record FollowCreateRequest(
    UUID followerId,
    UUID followeeId
) {
}
