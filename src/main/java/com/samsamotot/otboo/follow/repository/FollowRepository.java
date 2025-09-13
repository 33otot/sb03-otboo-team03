package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepository
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Object findByFollowerIdAndFolloweeId(User followerId, User followeeId);

    boolean existsFollowByFollowerIdAndFolloweeId(User followerId, User followeeId);
}
