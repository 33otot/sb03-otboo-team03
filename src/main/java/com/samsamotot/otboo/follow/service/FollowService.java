package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowSummaryDto;
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


    /*
        팔로우 기능
     */
    FollowDto follow(FollowCreateRequest request);

    // 팔로우 요약 정보 조회
    FollowSummaryDto findFollowSummaries(UUID userId);

}
