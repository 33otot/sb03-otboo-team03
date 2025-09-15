package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.follow.dto.FollowSummaryDto;
import com.samsamotot.otboo.follow.dto.FollowingRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepositoryImpl
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowRepositoryCustom {

    @Override
    public long countTotalElements(FollowingRequest request) {
        return 0;
    }

    @Override
    public List<FollowSummaryDto> findFollowings(FollowingRequest request) {
        return List.of();
    }
}
