package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.follow.dto.*;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.service
 * FileName     : FollowService
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 * Description  : 팔로우 관련 비즈니스 로직을 작성하는 인터페이스
 */
@Validated
public interface FollowService {
    FollowDto follow(FollowCreateRequest request);

    FollowSummaryDto findFollowSummaries(UUID userId);

    FollowListResponse getFollowings(@Valid FollowingRequest request);

    FollowListResponse getFollowers(@Valid FollowingRequest request);

    void unfollow(UUID followId);
}
