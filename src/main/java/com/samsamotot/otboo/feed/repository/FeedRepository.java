package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.weather.entity.Grid;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {

    // 논리적으로 삭제되지 않는 피드를 id로 조회합니다.
    Optional<Feed> findByIdAndIsDeletedFalse(UUID id);

    // 논리적으로 삭제된 피드를 id로 조회합니다.
    Optional<Feed> findByIdAndIsDeletedTrue(UUID id);

    // 특정 피드의 좋아요 수를 1 증가시킵니다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        update feeds
           set like_count = like_count + 1,
               updated_at = CURRENT_TIMESTAMP
         where id = :feedId
        """, nativeQuery = true)
    int incrementLikeCount(@Param("feedId") UUID feedId);

    // 특정 피드의 좋아요 수를 1 감소시킵니다. (0 이하로 내려가지 않음)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        update feeds
           set like_count = like_count - 1,
               updated_at = CURRENT_TIMESTAMP
         where id = :feedId
            and like_count > 0
        """, nativeQuery = true)
    int decrementLikeCount(@Param("feedId") UUID feedId);

    // 특정 피드의 댓글 수를 1 증가시킵니다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        update feeds
           set comment_count = comment_count + 1,
               updated_at = CURRENT_TIMESTAMP
         where id = :feedId
        """, nativeQuery = true)
    int incrementCommentCount(@Param("feedId") UUID feedId);

    // 특정 피드의 댓글 수를 1 감소시킵니다. (0 이하로 내려가지 않음)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        update feeds
           set comment_count = comment_count - 1,
               updated_at = CURRENT_TIMESTAMP
         where id = :feedId
            and comment_count > 0
        """, nativeQuery = true)
    int decrementCommentCount(@Param("feedId") UUID feedId);
    
    // 특정 격자에서 피드가 참조하는 날씨 데이터 ID들을 조회합니다.
    @Query("select f.weather.id from Feed f where f.weather.grid = :grid and f.weather is not null")
    Set<UUID> findWeatherIdsByGrid(@Param("grid") Grid grid);

    Feed findByIdAndIsDeletedTrueAndAuthorId(UUID id, boolean isDeleted, UUID authorId);
}
