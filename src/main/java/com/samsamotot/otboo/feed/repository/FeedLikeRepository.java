package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.feed.entity.FeedLike;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {

    @Query("""
        select fl.feed.id
        from FeedLike fl
        where fl.user.id = :userId
          and fl.feed.id in :feedIds
    """)
    Set<UUID> findFeedLikeIdsByUserIdAndFeedIdIn(@Param("userId") UUID userId,
            @Param("feedIds")Collection<UUID> feedIds);

    boolean existsByFeedIdAndUserId(UUID feedId, UUID userId);
}
