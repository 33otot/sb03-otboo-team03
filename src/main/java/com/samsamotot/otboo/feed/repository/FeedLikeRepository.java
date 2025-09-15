package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedLike;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {

    Set<UUID> findFeedLikeIdsByUserIdAndFeedIdIn(UUID userId, Collection<UUID> feedIds);
}
