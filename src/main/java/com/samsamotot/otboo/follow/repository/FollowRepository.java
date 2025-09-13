package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.follow.entity.Follow;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepository
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 * Description  : 팔로우 기능 구현을 위한 JPA repository
 */
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsFollowByFollowerIdAndFolloweeId(@NotNull UUID uuid, @NotNull UUID uuid1);
}
