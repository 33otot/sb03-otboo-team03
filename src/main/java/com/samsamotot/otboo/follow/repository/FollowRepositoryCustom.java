package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.follow.dto.FollowingRequest;
import com.samsamotot.otboo.follow.entity.Follow;

import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepositoryCustom
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
public interface FollowRepositoryCustom {

    long countTotalElements(UUID userId, String nameLike);

    List<Follow> findFollowings(FollowingRequest request);
}
