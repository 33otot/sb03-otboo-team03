package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.follow.entity.Follow;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepository
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 * Description  : 팔로우 기능 구현을 위한 JPA repository
 */
public interface FollowRepository extends JpaRepository<Follow, UUID>, FollowRepositoryCustom {

    boolean existsByFollowerIdAndFolloweeId(@NotNull UUID followerId, @NotNull UUID followeeId);

    long countByFollowerId(UUID followerId);

    long countByFolloweeId(UUID followeeId);

    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
}