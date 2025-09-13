package com.samsamotot.otboo.follow.dto.user;

/**
 * PackageName  : com.samsamotot.otboo.follow.dto.user
 * FileName     : UserSummaryDto
 * Author       : dounguk
 * Date         : 2025. 9. 13.
 * Follow 개발을 위한 임시 UserSummaryDto
 */
public record UserSummaryDto(
    java.util.UUID userId,
    String name,
    String profileImageUrl
) {
}
