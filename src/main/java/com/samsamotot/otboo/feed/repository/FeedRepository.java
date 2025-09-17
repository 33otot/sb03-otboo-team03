package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.feed.entity.Feed;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {
    Optional<Feed> findByIdAndIsDeletedFalse(UUID id);
}
