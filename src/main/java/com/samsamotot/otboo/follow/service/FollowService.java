package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowListResponse;
import com.samsamotot.otboo.follow.dto.FollowingRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

/**
 * PackageName  : com.samsamotot.otboo.follow.service
 * FileName     : FollowService
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 * Description  : 팔로우 관련 비즈니스 로직을 작성하는 인터페이스
 */
@Validated
public interface FollowService {
    /*        팔로우 기능     */
    FollowDto follow(FollowCreateRequest request);

    /*        팔로잉 목록 조회     */
    FollowListResponse getFollowings(@Valid FollowingRequest request);

    /*            팔로워 목록 조회     */
    FollowListResponse getFollowers(@Valid FollowingRequest request);
}
