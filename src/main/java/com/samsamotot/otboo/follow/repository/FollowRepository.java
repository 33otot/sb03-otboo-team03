package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.follow.entity.Follow;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query("SELECT f.follower.id FROM Follow f WHERE f.followee.id = :followeeId")
    List<UUID> findFollowerIdsByFolloweeId(@Param("followeeId") UUID followeeId);
}