package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowSummaryDto;
import com.samsamotot.otboo.follow.dto.FollowingRequest;

import java.util.List;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepositoryCustom
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
public interface FollowRepositoryCustom {

    long countTotalElements(FollowingRequest request);

    List<FollowSummaryDto> findFollowings(FollowingRequest request);
}
