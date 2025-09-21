package com.samsamotot.otboo.follow.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto
 * FileName     : FollowSummaryDto
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Builder
public record FollowSummaryDto(
    UUID followeeId, // 팔로우 대상 사용자 ID
    Long followerCount, // 팔로워 수
    Long followingCount, // 팔로잉 수
    boolean followedByMe, // 내가 팔로우 대상 사용자를 팔로우 하고 있는지 여부
    UUID followedByMeId, // 내가 팔로우 대상 사용자를 팔로우하고 있는 팔로우 ID
    boolean followingMe // 대상 사용자가 나를 팔로우하고 있는지 여부
) {
}